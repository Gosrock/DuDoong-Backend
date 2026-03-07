package band.gosrock.infrastructure.config.ses

class SendRawEmailDto(
    val recipient: String,
    val subject: String,
    val bodyHtml: String,
) {
    // Replace sender@example.com with your "From" address.
    val sender: String = "공연 정산관리팀 <support@dudoong.com>"
    val rawEmailAttachments: MutableList<RawEmailAttachmentDto> = mutableListOf()

    fun addEmailAttachments(rawEmailAttachment: RawEmailAttachmentDto) {
        rawEmailAttachments.add(rawEmailAttachment)
    }

    class Builder {
        private var recipient: String = ""
        private var subject: String = ""
        private var bodyHtml: String = ""

        fun recipient(recipient: String) = apply { this.recipient = recipient }
        fun subject(subject: String) = apply { this.subject = subject }
        fun bodyHtml(bodyHtml: String) = apply { this.bodyHtml = bodyHtml }
        fun build() = SendRawEmailDto(recipient, subject, bodyHtml)
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
