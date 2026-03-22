package band.gosrock.domain.domains.user.domain

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.common.vo.PhoneNumberVo
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
class Profile(
    var name: String? = null,
    var email: String? = null,
    phoneNumber: String? = null,
    profileImage: String? = null,
) {
    @Embedded
    var phoneNumberVo: PhoneNumberVo? = PhoneNumberVo.valueOf(phoneNumber)

    @Embedded
    var profileImage: ImageVo? = ImageVo.valueOf(profileImage)

    fun withdraw() {
        this.name = "탈퇴한 유저"
        this.email = null
        this.phoneNumberVo = null
        this.profileImage = null
    }
}
