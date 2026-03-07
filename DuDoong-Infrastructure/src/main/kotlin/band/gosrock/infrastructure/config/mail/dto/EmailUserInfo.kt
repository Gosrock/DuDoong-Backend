package band.gosrock.infrastructure.config.mail.dto

data class EmailUserInfo(
    val name: String,
    val email: String,
    val receiveAgree: Boolean,
)
