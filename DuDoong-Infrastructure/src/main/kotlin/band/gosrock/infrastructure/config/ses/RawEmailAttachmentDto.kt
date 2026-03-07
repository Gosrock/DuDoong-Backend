package band.gosrock.infrastructure.config.ses

/** ses 로 raw email 보낼때 첨부파일의 내용을 지정할 수 있다. */
class RawEmailAttachmentDto(
    /** file type ex : application/pdf */
    val type: String,
    /**
     * will be ByteArrayDataSource
     * @see javax.mail.util.ByteArrayDataSource
     */
    val fileBytes: ByteArray,
    /** 사용자가 이메일을 받았을 때 뜰 첨부파일이름. ex : 이벤트_정산서.pdf */
    val fileName: String,
) {
    class Builder {
        private var type: String = ""
        private var fileBytes: ByteArray = byteArrayOf()
        private var fileName: String = ""

        fun type(type: String) = apply { this.type = type }
        fun fileBytes(fileBytes: ByteArray) = apply { this.fileBytes = fileBytes }
        fun fileName(fileName: String) = apply { this.fileName = fileName }
        fun build() = RawEmailAttachmentDto(type, fileBytes, fileName)
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
}
