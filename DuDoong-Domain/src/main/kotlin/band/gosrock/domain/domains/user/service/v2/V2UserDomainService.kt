package band.gosrock.domain.domains.user.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.InvalidUserNameException

@DomainService
class V2UserDomainService {
    /**
     * 마이페이지 프로필 부분 수정 (M-2, #729). null 인 값은 바꾸지 않는다.
     * - 닉네임: v1 이름 변경(`PATCH /v1/users/me/name`)과 같은 규칙 — 요청 검증(2~7자, 공백 아님)은 DTO, 저장은 v1 과 같은 [User.changeName].
     *   v1 도메인은 Kotlin `isNotBlank`(유니코드 공백 포함)로 한 번 더 막아 v1 에서는 500 이 나는 입력(예: 전각 공백만)을 v2 는 400 으로 돌려준다
     * - 프로필 이미지: 빈 문자열이면 기본 이미지(null). key 검증(본인 업로드 경로)은 호출자가 한다
     */
    fun updateProfile(user: User, name: String?, profileImageKey: String?) {
        name?.let {
            if (it.isBlank()) throw InvalidUserNameException.EXCEPTION
            user.changeName(it)
        }
        profileImageKey?.let { key -> user.profile?.profileImage = ImageVo.valueOf(key.ifEmpty { null }) }
    }
}
