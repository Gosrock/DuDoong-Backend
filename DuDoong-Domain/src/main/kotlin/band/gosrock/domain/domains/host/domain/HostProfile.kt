package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.vo.ImageVo
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded

@Embeddable
class HostProfile() {
    // 호스트 이름
    @Column(length = 15)
    var name: String? = null
        protected set

    // 간단 소개
    var introduce: String? = null
        protected set

    // 프로필 이미지 url
    @Embedded
    var profileImage: ImageVo? = null
        protected set

    // 대표자 이메일
    var contactEmail: String? = null
        protected set

    // 대표자 연락처
    @Column(length = 15)
    var contactNumber: String? = null
        protected set

    constructor(
        name: String?,
        introduce: String?,
        profileImageKey: String?,
        contactEmail: String?,
        contactNumber: String?,
    ) : this() {
        this.name = name
        this.introduce = introduce
        this.profileImage = ImageVo.valueOf(profileImageKey)
        this.contactEmail = contactEmail
        this.contactNumber = contactNumber
    }

    fun updateProfile(hostProfile: HostProfile) {
        this.profileImage = hostProfile.profileImage
        this.introduce = hostProfile.introduce
        this.contactEmail = hostProfile.contactEmail
        this.contactNumber = hostProfile.contactNumber
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var name: String? = null
        private var introduce: String? = null
        private var profileImageKey: String? = null
        private var contactEmail: String? = null
        private var contactNumber: String? = null

        fun name(name: String?) = apply { this.name = name }
        fun introduce(introduce: String?) = apply { this.introduce = introduce }
        fun profileImageKey(key: String?) = apply { this.profileImageKey = key }
        fun contactEmail(email: String?) = apply { this.contactEmail = email }
        fun contactNumber(number: String?) = apply { this.contactNumber = number }
        fun build() = HostProfile(name, introduce, profileImageKey, contactEmail, contactNumber)
    }
}
