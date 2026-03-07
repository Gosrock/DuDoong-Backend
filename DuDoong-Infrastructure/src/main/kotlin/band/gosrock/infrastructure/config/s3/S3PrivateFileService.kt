package band.gosrock.infrastructure.config.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class S3PrivateFileService(
    private val amazonS3: AmazonS3,
    @Value("\${aws.s3.private-bucket}") private val bucket: String,
    @Value("\${aws.s3.base-url}") private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(S3PrivateFileService::class.java)

    companion object {
        private const val eventOrdersExcelFileName = "eventOrders.xlsx"
        private const val eventSettlementPdfFileName = "eventSettlement.pdf"
    }

    fun eventOrdersExcelUpload(eventId: Long, outputStream: ByteArrayOutputStream): String {
        val bytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(bytes)
        val fileKey = eventOrdersExcelGetKey(eventId)
        amazonS3.putObject(bucket, fileKey, inputStream, getExcelObjectMetadata(bytes.size))
        return fileKey
    }

    fun eventSettlementPdfUpload(eventId: Long, outputStream: ByteArrayOutputStream): String {
        val bytes = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(bytes)
        val fileKey = getEventSettlementPdfKey(eventId)
        amazonS3.putObject(bucket, fileKey, inputStream, getPdfObjectMetadata(bytes.size))
        return fileKey
    }

    private fun eventOrdersExcelGetKey(eventId: Long): String =
        "$baseUrl/event/$eventId/$eventOrdersExcelFileName"

    private fun getEventSettlementPdfKey(eventId: Long): String =
        "$baseUrl/event/$eventId/$eventSettlementPdfFileName"

    private fun getExcelObjectMetadata(contentLength: Int): ObjectMetadata {
        val objectMetadata = ObjectMetadata()
        objectMetadata.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        objectMetadata.contentLength = contentLength.toLong()
        return objectMetadata
    }

    private fun getPdfObjectMetadata(contentLength: Int): ObjectMetadata {
        val objectMetadata = ObjectMetadata()
        objectMetadata.contentType = "application/pdf"
        objectMetadata.contentLength = contentLength.toLong()
        return objectMetadata
    }

    fun downloadEventSettlementPdf(eventId: Long): ByteArray {
        val obj = amazonS3.getObject(bucket, getEventSettlementPdfKey(eventId))
        return try {
            obj.objectContent.readAllBytes()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun downloadEventOrdersExcel(eventId: Long): ByteArray {
        val obj = amazonS3.getObject(bucket, eventOrdersExcelGetKey(eventId))
        return try {
            obj.objectContent.readAllBytes()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
