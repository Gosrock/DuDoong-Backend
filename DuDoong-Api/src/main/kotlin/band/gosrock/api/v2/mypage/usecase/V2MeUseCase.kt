package band.gosrock.api.v2.mypage.usecase

import band.gosrock.api.v2.mypage.dto.request.V2MeImageUploadRequest
import band.gosrock.api.v2.mypage.dto.request.V2UpdateMeRequest
import band.gosrock.api.v2.mypage.dto.response.V2MeImageUploadResponse
import band.gosrock.api.v2.mypage.dto.response.V2MeResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.service.v2.V2MyPageHostQuery
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.InvalidUserProfileImageKeyException
import band.gosrock.domain.domains.user.service.v2.V2UserDomainService
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import org.springframework.transaction.annotation.Transactional

/** M-1 ~ M-3 내 프로필 (#729). 항상 요청자 본인(토큰의 userId)만 다룬다 */
@UseCase
class V2MeUseCase(
    private val userAdaptor: UserAdaptor,
    private val v2MyPageHostQuery: V2MyPageHostQuery,
    private val v2UserDomainService: V2UserDomainService,
    private val presignedUrlService: S3UploadPresignedUrlService,
) {
    @Transactional(readOnly = true)
    fun read(userId: Long): V2MeResponse = toResponse(userAdaptor.queryUser(userId))

    @Transactional
    fun update(userId: Long, request: V2UpdateMeRequest): V2MeResponse {
        validateImageKey(userId, request.profileImageKey)
        val user = userAdaptor.queryUser(userId)
        v2UserDomainService.updateProfile(user, request.name, request.profileImageKey)
        return toResponse(user)
    }

    /** 기존 유저 이미지 경로(user/{userId}/...)와 확장자(JPEG·JPG·PNG) 그대로. 크기 제한은 기존 업로드와 같이 서버에서 두지 않는다 */
    fun getImageUploadUrl(userId: Long, request: V2MeImageUploadRequest): V2MeImageUploadResponse =
        V2MeImageUploadResponse.of(presignedUrlService.forUser(userId, request.extension!!))

    private fun toResponse(user: User): V2MeResponse {
        val userId = user.id!!
        return V2MeResponse.of(user, v2MyPageHostQuery.findMyHosts(userId, MAX_HOST_SUMMARY), v2MyPageHostQuery.countMyHosts(userId))
    }

    /** 빈 문자열(기본 이미지)이거나, M-3 이 이 유저에게 발급한 key 여야 한다. 외부 URL / 다른 유저 key 거부 */
    private fun validateImageKey(userId: Long, key: String?) {
        if (key == null || key.isEmpty()) return
        if (!key.startsWith(presignedUrlService.userImageKeyPrefix(userId)) || key.contains("..")) {
            throw InvalidUserProfileImageKeyException.EXCEPTION
        }
    }

    companion object {
        const val MAX_HOST_SUMMARY = 10
    }
}
