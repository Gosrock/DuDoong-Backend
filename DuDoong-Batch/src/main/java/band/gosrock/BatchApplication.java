package band.gosrock;


import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

@EnableBatchProcessing
public class BatchApplication {
    public static void main(String[] args) {
        final var context =
                new SpringApplicationBuilder(BatchApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(args);
        System.exit(SpringApplication.exit(context));
    }
}
