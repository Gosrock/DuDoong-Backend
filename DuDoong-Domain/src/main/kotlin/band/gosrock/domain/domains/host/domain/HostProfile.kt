package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.vo.ImageVo
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
class HostProfile(
    // 호스트 이름
    @Column(length = 15)
    var name: String? = null,
    // 간단 소개
    var introduce: String? = null,
    profileImageKey: String? = null,
    // 대표자 이메일
    var contactEmail: String? = null,
    // 대표자 연락처
    @Column(length = 15)
    var contactNumber: String? = null,
) {
    // 프로필 이미지 url
    @Embedded
    var profileImage: ImageVo? = ImageVo.valueOf(profileImageKey)

    fun updateProfile(hostProfile: HostProfile) {
        this.name = hostProfile.name
        this.profileImage = hostProfile.profileImage
        this.introduce = hostProfile.introduce
        this.contactEmail = hostProfile.contactEmail
        this.contactNumber = hostProfile.contactNumber
    }
}
