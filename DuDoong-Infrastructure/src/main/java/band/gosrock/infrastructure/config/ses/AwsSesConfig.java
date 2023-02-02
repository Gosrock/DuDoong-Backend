package band.gosrock.infrastructure.config.ses;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class AwsSesConfig {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Bean
    public SesClient sesClient() {
        AwsBasicCredentials awsBasicCredentials =
                AwsBasicCredentials.create(this.accessKey, this.secretKey);
        return SesClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(Region.US_WEST_2)
                .build();
    }
}
