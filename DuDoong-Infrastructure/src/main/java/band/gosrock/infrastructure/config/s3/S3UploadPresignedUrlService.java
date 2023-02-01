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

    public ImageUrlDto forUser(Long userId, String fileExtension) {
        String fixedFileExtension = getFixedFileExtension(fileExtension);
        String fileName = getForUserFileName(userId, fixedFileExtension);
        log.info(fileName);
        URL url =
                amazonS3.generatePresignedUrl(
                        getGeneratePreSignedUrlRequest(bucket, fileName, fixedFileExtension));
        return ImageUrlDto.of(url.toString(), fileName);
    }

    public ImageUrlDto forHost(Long hostId, String fileExtension) {
        String fixedFileExtension = getFixedFileExtension(fileExtension);
        String fileName = getForHostFileName(hostId, fixedFileExtension);
        log.info(fileName);
        URL url =
                amazonS3.generatePresignedUrl(
                        getGeneratePreSignedUrlRequest(bucket, fileName, fixedFileExtension));
        return ImageUrlDto.of(url.toString(), fileName);
    }

    public ImageUrlDto forEvent(Long eventId, String fileExtension) {
        String fixedFileExtension = getFixedFileExtension(fileExtension);
        String fileName = getForEventFileName(eventId, fixedFileExtension);
        log.info(fileName);
        URL url =
                amazonS3.generatePresignedUrl(
                        getGeneratePreSignedUrlRequest(bucket, fileName, fixedFileExtension));
        return ImageUrlDto.of(url.toString(), fileName);
    }

    private String getFixedFileExtension(String fileExtension) {
        validFileExtension(fileExtension);
        return changeJpgToJpeg(fileExtension);
    }

    private String getForUserFileName(Long userId, String fileExtension) {
        return baseUrl
                + "/user/"
                + userId.toString()
                + "/"
                + UUID.randomUUID()
                + "."
                + fileExtension;
    }

    private String getForHostFileName(Long hostId, String fileExtension) {
        return baseUrl
                + "/host/"
                + hostId.toString()
                + "/"
                + UUID.randomUUID()
                + "."
                + fileExtension;
    }

    private String getForEventFileName(Long eventId, String fileExtension) {
        return baseUrl
                + "/event/"
                + eventId.toString()
                + "/"
                + UUID.randomUUID()
                + "."
                + fileExtension;
    }

    private String changeJpgToJpeg(String fileExtension) {
        if (fileExtension.equals("jpg")) {
            return "jpeg";
        }
        return fileExtension;
    }

    private static void validFileExtension(String fileExtension) {
        if (!(fileExtension.equals("jpg")
                || fileExtension.equals("jpeg")
                || fileExtension.equals("png"))) {
            throw BadFileExtensionException.EXCEPTION;
        }
    }

    private GeneratePresignedUrlRequest getGeneratePreSignedUrlRequest(
            String bucket, String fileName, String fileExtension) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName)
                        .withMethod(HttpMethod.PUT)
                        .withKey(fileName)
                        .withContentType("image/" + fileExtension)
                        .withExpiration(getPreSignedUrlExpiration());
        generatePresignedUrlRequest.addRequestParameter(
                Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());
        return generatePresignedUrlRequest;
    }

    private Date getPreSignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        // 3분
        expTimeMillis += 1000 * 60 * 3;
        expiration.setTime(expTimeMillis);
        return expiration;
    }
}
