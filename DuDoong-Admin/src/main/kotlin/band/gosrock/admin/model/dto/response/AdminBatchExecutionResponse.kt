package band.gosrock.admin.model.dto.response

import band.gosrock.admin.repository.BatchExecutionRow
import java.time.Duration
import java.time.LocalDateTime

data class AdminBatchExecutionResponse(
    val executionId: Long,
    val jobName: String,
    val status: String,
    val exitCode: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    /** 초, 소수 첫째 자리 (배치가 1~2초라 정수로 자르면 정보가 사라진다) */
    val durationSeconds: Double?,
    val exitMessage: String?,
    val parameters: Map<String, String>,
) {
    companion object {
        const val EXIT_MESSAGE_MAX_LENGTH = 500

        fun durationSecondsOf(start: LocalDateTime?, end: LocalDateTime?): Double? {
            if (start == null || end == null) return null
            return Math.round(Duration.between(start, end).toMillis() / 100.0) / 10.0
        }

        fun of(row: BatchExecutionRow): AdminBatchExecutionResponse =
            AdminBatchExecutionResponse(
                executionId = row.executionId,
                jobName = row.jobName,
                status = row.status,
                exitCode = row.exitCode,
                startTime = row.startTime,
                endTime = row.endTime,
                durationSeconds = durationSecondsOf(row.startTime, row.endTime),
                exitMessage = row.exitMessage?.takeIf { it.isNotBlank() }?.take(EXIT_MESSAGE_MAX_LENGTH),
                parameters = row.parameters,
            )
    }
}
