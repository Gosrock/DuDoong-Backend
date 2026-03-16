package band.gosrock.domain.domains.user.domain

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.common.vo.PhoneNumberVo
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
class Profile() {
    var name: String? = null
        protected set
    var email: String? = null
        protected set

    @Embedded
    var phoneNumberVo: PhoneNumberVo? = null
        protected set

    @Embedded
    var profileImage: ImageVo? = null
        protected set

    constructor(name: String, email: String, phoneNumber: String?, profileImage: String?) : this() {
        this.name = name
        this.email = email
        this.phoneNumberVo = PhoneNumberVo.valueOf(phoneNumber)
        this.profileImage = ImageVo.valueOf(profileImage)
    }

    fun withdraw() {
        this.name = "탈퇴한 유저"
        this.email = null
        this.phoneNumberVo = null
        this.profileImage = null
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var name: String = ""
        private var email: String = ""
        private var phoneNumber: String? = null
        private var profileImage: String? = null

        fun name(name: String) = apply { this.name = name }
        fun email(email: String) = apply { this.email = email }
        fun phoneNumber(phoneNumber: String?) = apply { this.phoneNumber = phoneNumber }
        fun profileImage(profileImage: String?) = apply { this.profileImage = profileImage }
        fun build() = Profile(name, email, phoneNumber, profileImage)
    }
}
