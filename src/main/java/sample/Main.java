package sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import sample.prepare.PrepareDataService;
import sample.test.TestService;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@EnableTransactionManagement
public class Main implements CommandLineRunner {
    private final PrepareDataService prepareDataService;
    private final TestService testService;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        prepareDataService.prepareData();
        testService.test();
    }
}