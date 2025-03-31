package sample.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("test")
@Setter
@Getter
public class TestProperties {
    private int threads = 40;
    private int batchSize = 10_000;
    private Duration duration = Duration.ofMinutes(7);
    private String failPath = "target/failures";
}
