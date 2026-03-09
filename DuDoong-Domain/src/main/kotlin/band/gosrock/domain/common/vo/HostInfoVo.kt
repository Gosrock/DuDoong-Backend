package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.host.domain.Host

class HostInfoVo private constructor(
    val hostId: Long?,
    val name: String?,
    val introduce: String?,
    val profileImage: ImageVo?,
    val contactEmail: String?,
    val contactNumber: String?,
    val partner: Boolean?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostInfoVo) return false
        return hostId == other.hostId && name == other.name && introduce == other.introduce &&
            profileImage == other.profileImage && contactEmail == other.contactEmail &&
            contactNumber == other.contactNumber && partner == other.partner
    }

    override fun hashCode(): Int = java.util.Objects.hash(hostId, name, introduce, profileImage, contactEmail, contactNumber, partner)

    companion object {
        @JvmStatic
        fun from(host: Host): HostInfoVo =
            HostInfoVo(
                hostId = host.id,
                name = host.profile?.name,
                introduce = host.profile?.introduce,
                profileImage = host.profile?.profileImage,
                contactEmail = host.profile?.contactEmail,
                contactNumber = host.profile?.contactNumber,
                partner = host.partner,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var name: String? = null
        private var introduce: String? = null
        private var profileImage: ImageVo? = null
        private var contactEmail: String? = null
        private var contactNumber: String? = null
        private var partner: Boolean? = null

        fun hostId(v: Long?) = apply { hostId = v }
        fun name(v: String?) = apply { name = v }
        fun introduce(v: String?) = apply { introduce = v }
        fun profileImage(v: ImageVo?) = apply { profileImage = v }
        fun contactEmail(v: String?) = apply { contactEmail = v }
        fun contactNumber(v: String?) = apply { contactNumber = v }
        fun partner(v: Boolean?) = apply { partner = v }
        fun build() = HostInfoVo(hostId, name, introduce, profileImage, contactEmail, contactNumber, partner)
    }
}
