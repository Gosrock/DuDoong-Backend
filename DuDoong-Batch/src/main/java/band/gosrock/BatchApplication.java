package band.gosrock;


import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {
    public static void main(String[] args) {
        final var context = SpringApplication.run(BatchApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }
}
