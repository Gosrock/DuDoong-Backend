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
        /**
         * EXIT_MESSAGE 에는 스택트레이스가 통째로 들어가 DB 접속 정보·SQL·수신자 연락처가 섞일 수 있다.
         * 첫 줄(예외 클래스와 메시지)만 앞부분까지 보여 준다. 자세한 내용은 CloudWatch Logs 에서 본다
         */
        const val EXIT_MESSAGE_MAX_LENGTH = 200

        fun summarizeExitMessage(message: String?): String? =
            message?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }?.take(EXIT_MESSAGE_MAX_LENGTH)

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
                exitMessage = summarizeExitMessage(row.exitMessage),
                parameters = row.parameters,
            )
    }
}
