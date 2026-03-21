package band.gosrock.admin.model.dto.request

import band.gosrock.domain.domains.host.domain.HostRole

data class AdminAddHostMemberRequest(
    val userId: Long,
    val role: HostRole,
)
