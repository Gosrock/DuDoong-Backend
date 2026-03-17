package band.gosrock.admin.model.dto.request

import band.gosrock.domain.domains.user.domain.AccountState

data class AdminUpdateUserStatusRequest(
    val status: AccountState,
)
