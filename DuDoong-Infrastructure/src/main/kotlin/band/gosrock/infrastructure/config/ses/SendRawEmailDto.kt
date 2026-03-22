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
}
