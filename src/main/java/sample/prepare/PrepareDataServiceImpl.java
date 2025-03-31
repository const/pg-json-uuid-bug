package sample.prepare;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sample.dto.CompanyDto;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrepareDataServiceImpl implements PrepareDataService {
    private static final int TOTAL = 30_000;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void prepareData() throws Exception {
        jdbcTemplate.execute("create extension if not exists tsm_system_rows;");
        var ddl = IOUtils.resourceToString("/db.sql", StandardCharsets.UTF_8);
        jdbcTemplate.execute(ddl);
        transactionTemplate.executeWithoutResult(tx -> {
            var dataExits = jdbcTemplate.query("select 1 from company limit 1", ResultSet::next);
            if (dataExits == null || !dataExits) {
                log.info("No data detected, generating data");
                var session = new PrepareDataSession();
                var data = session.prepareBatch(TOTAL);
                log.info("Saving data");
                save(data);
            }
        });

    }

    private void save(List<CompanyDto> data) {
        saveCompany(data);
        saveVerification(data);
        saveOffices(data);
        saveContactPersons(data);
        saveContactDetails(data);
    }

    private void saveContactDetails(List<CompanyDto> data) {
        var sql = """
                INSERT INTO public.contact_detail
                (contact_person_id, contact_type, value)
                VALUES(?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .flatMap(c -> c.getContactPersons().stream())
                .flatMap(p -> p.getDetails()
                        .stream()
                        .map(d -> new Object[]{
                                p.getId(),
                                d.getType().name(),
                                d.getValue()
                        })
                ).toList());
        log.info("Saved {} contact details", IntStream.of(r).sum());
    }

    private void saveContactPersons(List<CompanyDto> data) {
        var sql = """
                INSERT INTO public.contact_person
                (contact_person_id, "name", "position", company_id)
                VALUES(?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .flatMap(c -> c.getContactPersons()
                        .stream()
                        .map(p -> new Object[]{
                                p.getId(),
                                p.getName(),
                                p.getPosition(),
                                c.getId()
                        })
                ).toList());
        log.info("Saved {} contact persons", IntStream.of(r).sum());
    }

    private void saveOffices(List<CompanyDto> data) {
        var sql = """
                INSERT INTO public.company_office
                (office_id, address, city, "name", company_id)
                VALUES(?, ?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .flatMap(c -> c.getOffices()
                        .stream()
                        .map(o -> new Object[]{
                                o.getId(),
                                o.getAddress(),
                                o.getCity(),
                                o.getName(),
                                c.getId()
                        })
                ).toList());
        log.info("Saved {} offices", IntStream.of(r).sum());
    }

    private void saveVerification(List<CompanyDto> data) {
        var sql = """
                INSERT INTO public.verification_info
                ("comment", status, "timestamp", username, company_id)
                VALUES(?, ?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .filter(c -> c.getVerified() != null)
                .map(c ->
                        new Object[]{
                                c.getVerified().getComment(),
                                c.getVerified().getStatus().name(),
                                c.getVerified().getTimestamp().atOffset(ZoneOffset.UTC),
                                c.getVerified().getUser(),
                                c.getId()}
                ).toList());
        log.info("Saved {} verifications", IntStream.of(r).sum());
    }

    private void saveCompany(List<CompanyDto> data) {
        var sql = """
                INSERT INTO public.company
                (company_id, description, industry, "name", url)
                VALUES(?, ?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .map(c ->
                        new Object[]{c.getId(), c.getDescription(), c.getIndustry(), c.getName(), c.getUrl()}
                ).toList());
        log.info("Saved {} companies", IntStream.of(r).sum());

    }

}
