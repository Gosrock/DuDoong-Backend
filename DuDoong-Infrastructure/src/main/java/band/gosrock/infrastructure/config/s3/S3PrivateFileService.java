package band.gosrock.infrastructure.config.s3;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3PrivateFileService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.private-bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    private static String eventOrdersExcelFileName = "eventOrders.xlsx";
    private static String eventSettlementPdfFileName = "eventSettlement.pdf";

    public String eventOrdersExcelUpload(Long eventId, ByteArrayOutputStream outputStream) {
        byte[] bytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        String fileKey = eventOrdersExcelGetKey(eventId);

        amazonS3.putObject(bucket, fileKey, inputStream, getExcelObjectMetadata(bytes.length));
        return fileKey;
    }

    public String eventSettlementPdfUpload(Long eventId, ByteArrayOutputStream outputStream) {
        byte[] bytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        String fileKey = getEventSettlementPdfKey(eventId);
        amazonS3.putObject(bucket, fileKey, inputStream, getPdfObjectMetadata(bytes.length));
        return fileKey;
    }

    private String eventOrdersExcelGetKey(Long eventId) {
        return baseUrl + "/event/" + eventId.toString() + "/" + eventOrdersExcelFileName;
    }

    private String getEventSettlementPdfKey(Long eventId) {
        return baseUrl + "/event/" + eventId.toString() + "/" + eventSettlementPdfFileName;
    }

    private ObjectMetadata getExcelObjectMetadata(int contentLength) {
        // create metadata for uploading file
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        objectMetadata.setContentLength(contentLength);
        return objectMetadata;
    }

    private ObjectMetadata getPdfObjectMetadata(int contentLength) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("application/pdf");
        objectMetadata.setContentLength(contentLength);
        return objectMetadata;
    }

    public byte[] downloadEventSettlementPdf(Long eventId) throws IOException {
        S3Object object = amazonS3.getObject(bucket, getEventSettlementPdfKey(eventId));

        S3ObjectInputStream finalObject = object.getObjectContent();
        return finalObject.readAllBytes();
    }

    public byte[] downloadEventOrdersExcel(Long eventId) throws IOException {
        S3Object object = amazonS3.getObject(bucket, eventOrdersExcelGetKey(eventId));
        S3ObjectInputStream finalObject = object.getObjectContent();
        return finalObject.readAllBytes();
    }
}
