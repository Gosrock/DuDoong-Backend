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

    /** forUser 가 발급하는 유저(프로필) 이미지 key 의 공통 prefix */
    fun userImageKeyPrefix(userId: Long): String = "$baseUrl/user/$userId/"

    private fun getForUserFileName(userId: Long, fileExtension: String): String =
        "${userImageKeyPrefix(userId)}${UUID.randomUUID()}.$fileExtension"

    /** forHost 가 발급하는 호스트 이미지 key 의 공통 prefix */
    fun hostImageKeyPrefix(hostId: Long): String = "$baseUrl/host/$hostId/"

    private fun getForHostFileName(hostId: Long, fileExtension: String): String =
        "${hostImageKeyPrefix(hostId)}${UUID.randomUUID()}.$fileExtension"

    /** forEvent 가 발급하는 공연 이미지 key 의 공통 prefix */
    fun eventImageKeyPrefix(eventId: Long): String = "$baseUrl/event/$eventId/"

    private fun getForEventFileName(eventId: Long, fileExtension: String): String =
        "${eventImageKeyPrefix(eventId)}${UUID.randomUUID()}.$fileExtension"

    /** forUser 가 발급한 형식 그대로인 key 인지 (`{prefix}{UUID}.{jpeg|jpg|png}`) */
    fun isUserImageKey(userId: Long, key: String): Boolean = isIssuedKey(userImageKeyPrefix(userId), key)

    /** forHost 가 발급한 형식 그대로인 key 인지 */
    fun isHostImageKey(hostId: Long, key: String): Boolean = isIssuedKey(hostImageKeyPrefix(hostId), key)

    /** forEvent 가 발급한 형식 그대로인 key 인지 */
    fun isEventImageKey(eventId: Long, key: String): Boolean = isIssuedKey(eventImageKeyPrefix(eventId), key)

    /**
     * 화이트리스트 검사: prefix 뒤가 정확히 UUID + 허용 확장자여야 한다. `..`·`%2e%2e`·하위 경로·쿼리스트링·다른 확장자는 모두 거부.
     * prefix(baseUrl 포함)는 정규식이 아니라 문자열로 비교한다
     */
    private fun isIssuedKey(prefix: String, key: String): Boolean =
        key.startsWith(prefix) && ISSUED_FILE_NAME.matches(key.substring(prefix.length))

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

    companion object {
        private val ISSUED_FILE_NAME = Regex("[0-9a-f-]{36}\\.(jpeg|jpg|png)")
    }
}
