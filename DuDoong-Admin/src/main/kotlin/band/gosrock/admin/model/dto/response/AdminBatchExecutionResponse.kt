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
    val durationSeconds: Long?,
    val exitMessage: String?,
    val parameters: Map<String, String>,
) {
    companion object {
        const val EXIT_MESSAGE_MAX_LENGTH = 500

        fun of(row: BatchExecutionRow): AdminBatchExecutionResponse =
            AdminBatchExecutionResponse(
                executionId = row.executionId,
                jobName = row.jobName,
                status = row.status,
                exitCode = row.exitCode,
                startTime = row.startTime,
                endTime = row.endTime,
                durationSeconds = if (row.startTime != null && row.endTime != null) Duration.between(row.startTime, row.endTime).seconds else null,
                exitMessage = row.exitMessage?.takeIf { it.isNotBlank() }?.take(EXIT_MESSAGE_MAX_LENGTH),
                parameters = row.parameters,
            )
    }
}
