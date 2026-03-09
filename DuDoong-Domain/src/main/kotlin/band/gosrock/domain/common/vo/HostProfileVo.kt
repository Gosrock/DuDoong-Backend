package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.host.domain.Host

class HostProfileVo private constructor(
    val hostId: Long?,
    val name: String?,
    val introduce: String?,
    val profileImage: ImageVo?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostProfileVo) return false
        return hostId == other.hostId && name == other.name && introduce == other.introduce && profileImage == other.profileImage
    }

    override fun hashCode(): Int = java.util.Objects.hash(hostId, name, introduce, profileImage)

    companion object {
        @JvmStatic
        fun from(host: Host): HostProfileVo =
            HostProfileVo(
                hostId = host.id,
                name = host.profile?.name,
                introduce = host.profile?.introduce,
                profileImage = host.profile?.profileImage,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var name: String? = null
        private var introduce: String? = null
        private var profileImage: ImageVo? = null

        fun hostId(v: Long?) = apply { hostId = v }
        fun name(v: String?) = apply { name = v }
        fun introduce(v: String?) = apply { introduce = v }
        fun profileImage(v: ImageVo?) = apply { profileImage = v }
        fun build() = HostProfileVo(hostId, name, introduce, profileImage)
    }
}
