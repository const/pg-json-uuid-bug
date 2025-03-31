package sample.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("test")
@Setter
@Getter
public class TestProperties {
    private int threads = 10;
    private int batchSize = 1000;
    private Duration duration = Duration.ofMinutes(3);
    private String failPath = "target/failures";
}
