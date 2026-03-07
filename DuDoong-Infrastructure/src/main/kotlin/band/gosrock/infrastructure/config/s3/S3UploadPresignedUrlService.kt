package band.gosrock.infrastructure.config.s3

import band.gosrock.common.exception.BadFileExtensionException
import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.util.Date
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class S3UploadPresignedUrlService(
    private val amazonS3: AmazonS3,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.base-url}") private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(S3UploadPresignedUrlService::class.java)

    fun forUser(userId: Long, fileExtension: ImageFileExtension): ImageUrlDto {
        val fixedFileExtension = fileExtension.uploadExtension
        val fileName = getForUserFileName(userId, fixedFileExtension)
        log.info(fileName)
        val url = amazonS3.generatePresignedUrl(getGeneratePreSignedUrlRequest(bucket, fileName, fixedFileExtension))
        return ImageUrlDto.of(url.toString(), fileName)
    }

    fun forHost(hostId: Long, fileExtension: ImageFileExtension): ImageUrlDto {
        val fixedFileExtension = fileExtension.uploadExtension
        val fileName = getForHostFileName(hostId, fixedFileExtension)
        log.info(fileName)
        val url = amazonS3.generatePresignedUrl(getGeneratePreSignedUrlRequest(bucket, fileName, fixedFileExtension))
        return ImageUrlDto.of(url.toString(), fileName)
    }

    fun forEvent(eventId: Long, fileExtension: ImageFileExtension): ImageUrlDto {
        val fixedFileExtension = fileExtension.uploadExtension
        val fileName = getForEventFileName(eventId, fixedFileExtension)
        log.info(fileName)
        val url = amazonS3.generatePresignedUrl(getGeneratePreSignedUrlRequest(bucket, fileName, fixedFileExtension))
        return ImageUrlDto.of(url.toString(), fileName)
    }

    private fun getForUserFileName(userId: Long, fileExtension: String): String =
        "$baseUrl/user/$userId/${UUID.randomUUID()}.$fileExtension"

    private fun getForHostFileName(hostId: Long, fileExtension: String): String =
        "$baseUrl/host/$hostId/${UUID.randomUUID()}.$fileExtension"

    private fun getForEventFileName(eventId: Long, fileExtension: String): String =
        "$baseUrl/event/$eventId/${UUID.randomUUID()}.$fileExtension"

    private fun getGeneratePreSignedUrlRequest(
        bucket: String,
        fileName: String,
        fileExtension: String,
    ): GeneratePresignedUrlRequest {
        val generatePresignedUrlRequest =
            GeneratePresignedUrlRequest(bucket, fileName)
                .withMethod(HttpMethod.PUT)
                .withKey(fileName)
                .withContentType("image/$fileExtension")
                .withExpiration(getPreSignedUrlExpiration())
        generatePresignedUrlRequest.addRequestParameter(
            Headers.S3_CANNED_ACL,
            CannedAccessControlList.PublicRead.toString(),
        )
        return generatePresignedUrlRequest
    }

    private fun getPreSignedUrlExpiration(): Date {
        val expiration = Date()
        var expTimeMillis = expiration.time
        // 3분
        expTimeMillis += 1000 * 60 * 3
        expiration.time = expTimeMillis
        return expiration
    }
}
