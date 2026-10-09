package band.gosrock.admin.repository

import java.sql.ResultSet
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Repository

/** 잡별 최근 실행 요약 */
data class BatchJobSummaryRow(
    val jobName: String,
    val lastExecutionId: Long,
    val lastStatus: String,
    val lastStartTime: LocalDateTime?,
    val lastEndTime: LocalDateTime?,
    val lastSuccessTime: LocalDateTime?,
    val recentFailureCount: Long,
)

data class BatchExecutionRow(
    val executionId: Long,
    val jobName: String,
    val status: String,
    val exitCode: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val exitMessage: String?,
    val parameters: Map<String, String>,
)

/**
 * 배치 실행 이력 조회 (#757). Spring Batch 메타 테이블(BATCH_JOB_*)은 JPA 엔티티가 아니라서 JDBC 로 읽는다. 읽기 전용.
 * 시각(DATETIME)은 배치 JVM 이 KST 로 기록하므로 타임존 변환 없이 LocalDateTime 으로 그대로 읽는다.
 */
@Repository
class AdminBatchExecutionQuery(private val jdbcTemplate: JdbcTemplate) {

    /** 잡 이름별 마지막 실행, 마지막 성공(COMPLETED) 시작 시각, [failureSince] 이후 시작한 FAILED 실행 수 */
    fun findJobSummaries(failureSince: LocalDateTime): List<BatchJobSummaryRow> {
        val aggregates = jdbcTemplate.query(
            "SELECT i.JOB_NAME, MAX(e.JOB_EXECUTION_ID), " +
                "MAX(CASE WHEN e.STATUS = 'COMPLETED' THEN e.START_TIME END), " +
                "SUM(CASE WHEN e.STATUS = 'FAILED' AND e.START_TIME >= ? THEN 1 ELSE 0 END) " +
                "FROM BATCH_JOB_EXECUTION e JOIN BATCH_JOB_INSTANCE i ON i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID " +
                "GROUP BY i.JOB_NAME",
            { rs, _ -> JobAggregate(rs.getString(1), rs.getLong(2), rs.localDateTime(3), rs.getLong(4)) },
            failureSince,
        )
        if (aggregates.isEmpty()) return emptyList()
        val lastExecutions = jdbcTemplate.query(
            "SELECT JOB_EXECUTION_ID, STATUS, START_TIME, END_TIME FROM BATCH_JOB_EXECUTION " +
                "WHERE JOB_EXECUTION_ID IN (${placeholders(aggregates.size)})",
            { rs, _ -> LastExecution(rs.getLong(1), rs.getString(2), rs.localDateTime(3), rs.localDateTime(4)) },
            *aggregates.map { it.lastExecutionId }.toTypedArray(),
        ).associateBy { it.executionId }
        return aggregates.mapNotNull { aggregate ->
            lastExecutions[aggregate.lastExecutionId]?.let { last ->
                BatchJobSummaryRow(
                    jobName = aggregate.jobName,
                    lastExecutionId = last.executionId,
                    lastStatus = last.status,
                    lastStartTime = last.startTime,
                    lastEndTime = last.endTime,
                    lastSuccessTime = aggregate.lastSuccessTime,
                    recentFailureCount = aggregate.recentFailureCount,
                )
            }
        }
    }

    /** 실행 이력을 최신순(JOB_EXECUTION_ID DESC)으로 페이지 조회. [jobName] 이 있으면 정확히 일치하는 잡만. pageable 의 정렬은 쓰지 않는다 */
    fun findExecutions(jobName: String?, pageable: Pageable): Page<BatchExecutionRow> {
        val where = if (jobName == null) "" else "WHERE i.JOB_NAME = ? "
        val filterArgs = listOfNotNull(jobName)
        val total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION e JOIN BATCH_JOB_INSTANCE i ON i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID $where",
            Long::class.java,
            *filterArgs.toTypedArray(),
        ) ?: 0L
        if (total == 0L) return PageImpl(emptyList(), pageable, 0)
        val executions = jdbcTemplate.query(
            "SELECT e.JOB_EXECUTION_ID, i.JOB_NAME, e.STATUS, e.EXIT_CODE, e.START_TIME, e.END_TIME, e.EXIT_MESSAGE " +
                "FROM BATCH_JOB_EXECUTION e JOIN BATCH_JOB_INSTANCE i ON i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID " +
                "${where}ORDER BY e.JOB_EXECUTION_ID DESC LIMIT ? OFFSET ?",
            { rs, _ ->
                BatchExecutionRow(
                    executionId = rs.getLong(1),
                    jobName = rs.getString(2),
                    status = rs.getString(3),
                    exitCode = rs.getString(4),
                    startTime = rs.localDateTime(5),
                    endTime = rs.localDateTime(6),
                    exitMessage = rs.getString(7),
                    parameters = emptyMap(),
                )
            },
            *(filterArgs + listOf(pageable.pageSize, pageable.offset)).toTypedArray(),
        )
        val parameters = findParameters(executions.map { it.executionId })
        return PageImpl(executions.map { it.copy(parameters = parameters[it.executionId].orEmpty()) }, pageable, total)
    }

    /**
     * 실행들의 잡 파라미터를 한 번에 읽는다 (KEY_NAME -> 값 문자열).
     * Spring Batch 4 스키마 기준(TYPE_CD + STRING_VAL/DATE_VAL/LONG_VAL/DOUBLE_VAL). Batch 5 로 올리면 이 테이블이
     * PARAMETER_NAME/PARAMETER_TYPE/PARAMETER_VALUE 컬럼으로 바뀌므로 이 쿼리를 함께 고쳐야 한다.
     */
    private fun findParameters(executionIds: List<Long>): Map<Long, Map<String, String>> {
        if (executionIds.isEmpty()) return emptyMap()
        val result = linkedMapOf<Long, MutableMap<String, String>>()
        jdbcTemplate.query(
            "SELECT JOB_EXECUTION_ID, KEY_NAME, TYPE_CD, STRING_VAL, DATE_VAL, LONG_VAL, DOUBLE_VAL FROM BATCH_JOB_EXECUTION_PARAMS " +
                "WHERE JOB_EXECUTION_ID IN (${placeholders(executionIds.size)}) ORDER BY JOB_EXECUTION_ID, KEY_NAME",
            RowCallbackHandler { rs ->
                val value = when (rs.getString(3)) {
                    "DATE" -> rs.localDateTime(5)?.toString()
                    "LONG" -> rs.getLong(6).takeUnless { rs.wasNull() }?.toString()
                    "DOUBLE" -> rs.getDouble(7).takeUnless { rs.wasNull() }?.toString()
                    else -> rs.getString(4)
                }
                result.getOrPut(rs.getLong(1)) { linkedMapOf() }[rs.getString(2)] = value.orEmpty()
            },
            *executionIds.toTypedArray(),
        )
        return result
    }

    private fun ResultSet.localDateTime(column: Int): LocalDateTime? = getObject(column, LocalDateTime::class.java)

    private fun placeholders(n: Int) = List(n) { "?" }.joinToString(", ")

    private data class JobAggregate(val jobName: String, val lastExecutionId: Long, val lastSuccessTime: LocalDateTime?, val recentFailureCount: Long)

    private data class LastExecution(val executionId: Long, val status: String, val startTime: LocalDateTime?, val endTime: LocalDateTime?)
}
