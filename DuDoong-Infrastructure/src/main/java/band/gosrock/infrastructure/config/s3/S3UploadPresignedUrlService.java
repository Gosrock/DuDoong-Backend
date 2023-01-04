package band.gosrock.infrastructure.config.s3;

import band.gosrock.common.exception.BadFileExtensionException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadPresignedUrlService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    public String execute(Long userId, String fileExtension) {
        validFileExtension(fileExtension);
        String fileName =
            baseUrl
                + "/"
                + "u"
                + userId.toString()
                + "/"
                + UUID.randomUUID()
                + "."
                + fileExtension;
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
            getGeneratePreSignedUrlRequest(bucket, fileName);
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    private static void validFileExtension(String fileExtension) {
        if (!(fileExtension.equals("jpg")
            || fileExtension.equals("jpeg")
            || fileExtension.equals("png"))) {
            throw BadFileExtensionException.EXCEPTION;
        }
    }

    private GeneratePresignedUrlRequest getGeneratePreSignedUrlRequest(
        String bucket, String fileName) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
            new GeneratePresignedUrlRequest(bucket, fileName)
                .withMethod(HttpMethod.PUT)
                .withContentType("image/png")
                .withContentType("image/jpeg")
                .withExpiration(getPreSignedUrlExpiration());
        generatePresignedUrlRequest.addRequestParameter(
            Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());
        return generatePresignedUrlRequest;
    }

    private Date getPreSignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        // 한시간
        expTimeMillis += 1000 * 60 * 60;
        expiration.setTime(expTimeMillis);
        return expiration;
    }
}
