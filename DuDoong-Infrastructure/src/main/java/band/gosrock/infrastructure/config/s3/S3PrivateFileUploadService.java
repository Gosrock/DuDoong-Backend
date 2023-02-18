package band.gosrock.infrastructure.config.s3;


import band.gosrock.common.exception.BadFileExtensionException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
public class S3PrivateFileUploadService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.private-bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    private static String excelFileName = "eventOrderList.xlsx";

    public String excelUpload(Long eventId, ByteArrayOutputStream outputStream) {
        byte[] bytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        String fileKey = getFileKey(eventId);
        amazonS3.putObject(bucket, fileKey, inputStream, getExcelObjectMetadata(bytes.length));
        return fileKey;
    }

    private String getFileKey(Long eventId) {
        return baseUrl
            + "/event/"
            + eventId.toString()
            + "/"
            + excelFileName;
    }
    private ObjectMetadata getExcelObjectMetadata(int contentLength) {
        // create metadata for uploading file
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        objectMetadata.setContentLength(contentLength);
        return objectMetadata;
    }
}
