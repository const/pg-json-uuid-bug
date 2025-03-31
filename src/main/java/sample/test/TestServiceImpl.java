package sample.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sample.config.TestProperties;
import sample.dto.CompanyDto;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
@EnableConfigurationProperties(TestProperties.class)
@RequiredArgsConstructor
@Slf4j
public class TestServiceImpl implements TestService {
    private final TestProperties testProperties;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter folderNameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);
    private final AtomicInteger execution = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    public static final String SELECT_JSON_ARRAY = """
            with company_data as (
                select
                    json_build_object(
                        'id', c.company_id,
                        'name', c."name",
                        'industry', c.industry,
                        'description', c.description,
                        'url', c.url,
                        'verified', (
                            select
                                json_build_object(
                                    'timestamp', vi."timestamp",
                                    'status', vi.status,
                                    'comment', vi."comment",
                                    'user', vi.username
                                )
                            from verification_info vi where vi.company_id = c.company_id
                        ),
                        'contactPersons', (
                            select
                                coalesce(json_agg(
                                    json_build_object(
                                        'id', cp.contact_person_id,
                                        'name', cp.name,
                                        'position', cp.position,
                                        'details', (
                                            select
                                                coalesce(json_agg(
                                                    json_build_object(
                                                        'type', cd.contact_type,
                                                        'value', cd.value
                                                    )
                                                ), json_build_array())
                                            from contact_detail cd
                                            where cd.contact_person_id = cp.contact_person_id
                                        )
                                    )
                                ), json_build_array())
                            from contact_person cp
                            where cp.company_id = c.company_id
                        ),
                        'offices', (
                            select
                                coalesce(json_agg(
                                    json_build_object(
                                        'id', co.office_id,
                                        'name', co.name,
                                        'city', co.city,
                                        'address', co.address
                                    )
                                ), json_build_array())
                            from company_office co
                            where co.company_id = c.company_id
                        )
                    ) as company_dto
                from company c where c.company_id = any(?)
            )
            select coalesce(json_agg(company_dto), json_build_array()) from company_data;
            """;

    @Override
    public void test() {
        var now = Instant.now();
        var outPath = new File(testProperties.getFailPath(), folderNameFormat.format(now));
        //noinspection ResultOfMethodCallIgnored
        outPath.mkdirs();
        if (!outPath.isDirectory()) {
            log.error("Failed to create directory {}", outPath);
        }
        log.info("Saving results of test in the directory: {}", outPath.getAbsolutePath());
        var companyIds = transactionTemplate.execute(tx ->
                jdbcTemplate.query(
                        "select company_id from company",
                        (rs, i) -> rs.getObject(1, UUID.class)));
        assert companyIds != null;
        log.info("Found {} companies ", companyIds.size());
        Instant deadline = Instant.now().plus(testProperties.getDuration());
        var lock = new Semaphore(0);
        for (int i = 0; i < testProperties.getThreads(); i++) {
            new Thread(() -> {
                try {
                    runTest(deadline, companyIds, outPath);
                } finally {
                    lock.release();
                }
            }).start();
        }
        lock.acquireUninterruptibly(testProperties.getThreads());
        log.info("Run {} operations, detected {} failure(s)", execution.get(), failures.get());
    }

    private void runTest(Instant deadline, List<UUID> companyIds, File outPath) {
        var rnd = new Random();
        String[] json = new String[1];
        do {
            var n = execution.incrementAndGet();
            json[0] = null;
            try {
                var positions = new HashSet<Integer>();
                while (positions.size() < testProperties.getBatchSize()) {
                    positions.add(rnd.nextInt(companyIds.size()));
                }
                var positionArray = positions.stream().map(companyIds::get).toArray(UUID[]::new);
                transactionTemplate.executeWithoutResult(tx -> {
                    json[0] = jdbcTemplate.query(SELECT_JSON_ARRAY, rs -> {
                        if (!rs.next()) {
                            throw new IllegalStateException("Result set is empty");
                        }
                        return rs.getString(1);
                    }, (Object) positionArray);
                    try {
                        var parsed = objectMapper.readValue(json[0], new TypeReference<List<CompanyDto>>() {
                        });
                        assert parsed.size() == testProperties.getBatchSize();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex.toString(), ex);
                    }
                });
            } catch (Exception e) {
                failures.incrementAndGet();
                var name = "%09d".formatted(n);
                try {
                    StringWriter w = new StringWriter();
                    e.printStackTrace(new PrintWriter(w));
                    FileUtils.write(new File(outPath, name + ".log"), w.toString(), StandardCharsets.UTF_8);
                    if (json[0] != null) {
                        FileUtils.write(new File(outPath, name + ".json"), json[0], StandardCharsets.UTF_8);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to save failure", ex);
                }
            }

        } while (Instant.now().isBefore(deadline));
    }
}
