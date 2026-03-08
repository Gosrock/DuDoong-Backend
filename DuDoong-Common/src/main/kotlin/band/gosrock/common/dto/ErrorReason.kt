package band.gosrock.common.dto

data class ErrorReason(
    val status: Int,
    val code: String,
    val reason: String,
) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var status: Int = 0
        private var code: String = ""
        private var reason: String = ""

        fun status(status: Int) = apply { this.status = status }
        fun code(code: String) = apply { this.code = code }
        fun reason(reason: String) = apply { this.reason = reason }
        fun build() = ErrorReason(status, code, reason)
    }
}
