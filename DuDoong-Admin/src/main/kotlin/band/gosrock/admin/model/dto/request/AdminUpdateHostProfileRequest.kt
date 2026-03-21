package band.gosrock.admin.model.dto.request

data class AdminUpdateHostProfileRequest(
    val name: String?,
    val introduce: String?,
    val contactEmail: String?,
    val contactNumber: String?,
)
