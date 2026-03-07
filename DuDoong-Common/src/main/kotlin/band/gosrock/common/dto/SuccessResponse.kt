package band.gosrock.common.dto

import java.time.LocalDateTime

class SuccessResponse(
    val status: Int,
    val data: Any?,
) {
    val success: Boolean = true
    val timeStamp: LocalDateTime = LocalDateTime.now()
}
