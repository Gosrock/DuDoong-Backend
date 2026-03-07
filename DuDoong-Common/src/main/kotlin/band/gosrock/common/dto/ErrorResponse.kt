package band.gosrock.common.dto

import java.time.LocalDateTime

class ErrorResponse {
    val success: Boolean = false
    val status: Int
    val code: String
    val reason: String
    val timeStamp: LocalDateTime
    val path: String

    constructor(errorReason: ErrorReason, path: String) {
        this.status = errorReason.status
        this.code = errorReason.code
        this.reason = errorReason.reason
        this.timeStamp = LocalDateTime.now()
        this.path = path
    }

    constructor(status: Int, code: String, reason: String, path: String) {
        this.status = status
        this.code = code
        this.reason = reason
        this.timeStamp = LocalDateTime.now()
        this.path = path
    }
}
