package band.gosrock.admin.model.dto.request

import band.gosrock.domain.domains.user.domain.AccountRole

data class AdminUpdateUserRoleRequest(
    val role: AccountRole,
)
