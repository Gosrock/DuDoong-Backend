package band.gosrock.infrastructure.config.s3;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

//@SpringBootTest(classes = {S3Config.class, S3UploadPresignedUrlService.class})
//@ActiveProfiles("infrastructure")
//class S3UploadPresignedUrlServiceTest {
//
//    @Autowired
//    S3UploadPresignedUrlService s3UploadPresignedUrlService;
//
//    @Test
//    public void S3_url_발급테스트() {
//        String url = s3UploadPresignedUrlService.execute(1L,"jpg");
//    }
//}