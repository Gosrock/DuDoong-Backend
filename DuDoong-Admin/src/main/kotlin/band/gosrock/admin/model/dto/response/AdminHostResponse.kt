package band.gosrock.admin.model.dto.response

import band.gosrock.domain.domains.host.domain.Host
import java.time.LocalDateTime

data class AdminHostResponse(
    val id: Long,
    val name: String?,
    val introduce: String?,
    val contactEmail: String?,
    val contactNumber: String?,
    val profileImage: String?,
    val partner: Boolean,
    val masterUserId: Long?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(host: Host): AdminHostResponse =
            AdminHostResponse(
                id = host.id!!,
                name = host.profile?.name,
                introduce = host.profile?.introduce,
                contactEmail = host.profile?.contactEmail,
                contactNumber = host.profile?.contactNumber,
                profileImage = host.profile?.profileImage?.imageKey,
                partner = host.partner,
                masterUserId = host.masterUserId,
                createdAt = host.createdAt,
            )
    }
}
