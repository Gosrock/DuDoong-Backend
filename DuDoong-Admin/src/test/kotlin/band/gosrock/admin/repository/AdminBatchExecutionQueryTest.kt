package band.gosrock.admin.repository

import java.time.LocalDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

/** 배치 실행 이력 조회 (#757). H2(MySQL 모드)에 Spring Batch 4 메타 테이블을 만들어 실제 SQL 을 검증한다 */
@DisplayName("AdminBatchExecutionQuery")
class AdminBatchExecutionQueryTest {

    private val jdbcTemplate = JdbcTemplate(DriverManagerDataSource("jdbc:h2:mem:batch_meta;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""))
    private val query = AdminBatchExecutionQuery(jdbcTemplate)

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute(
            "CREATE TABLE BATCH_JOB_INSTANCE (JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY, VERSION BIGINT, " +
                "JOB_NAME VARCHAR(100) NOT NULL, JOB_KEY VARCHAR(32) NOT NULL)",
        )
        jdbcTemplate.execute(
            "CREATE TABLE BATCH_JOB_EXECUTION (JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY, VERSION BIGINT, " +
                "JOB_INSTANCE_ID BIGINT NOT NULL, CREATE_TIME DATETIME(6) NOT NULL, START_TIME DATETIME(6), END_TIME DATETIME(6), " +
                "STATUS VARCHAR(10), EXIT_CODE VARCHAR(2500), EXIT_MESSAGE VARCHAR(2500), LAST_UPDATED DATETIME(6), " +
                "JOB_CONFIGURATION_LOCATION VARCHAR(2500))",
        )
        jdbcTemplate.execute(
            "CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (JOB_EXECUTION_ID BIGINT NOT NULL, TYPE_CD VARCHAR(6) NOT NULL, " +
                "KEY_NAME VARCHAR(100) NOT NULL, STRING_VAL VARCHAR(250), DATE_VAL DATETIME(6), LONG_VAL BIGINT, " +
                "DOUBLE_VAL DOUBLE PRECISION, IDENTIFYING CHAR(1) NOT NULL)",
        )
        instance(1, "이벤트_자동만료")
        instance(2, "이벤트정산서")
        instance(3, "이벤트정산서")

        // 이벤트_자동만료: 성공 → 실패(최근) → 실행 중(마지막)
        execution(10, 1, "COMPLETED", at(1, 0), at(1, 1))
        execution(11, 1, "FAILED", at(5, 0), at(5, 2), "java.lang.IllegalStateException: boom")
        execution(13, 1, "STARTED", at(6, 0), null)
        // 이벤트정산서: 오래된 실패(7일 이전), 최근 성공 2건
        execution(12, 2, "FAILED", LocalDateTime.of(2026, 9, 20, 3, 0), LocalDateTime.of(2026, 9, 20, 3, 1))
        execution(14, 3, "COMPLETED", at(7, 0), at(7, 0, 30))
        execution(15, 3, "COMPLETED", at(8, 0), at(8, 1))

        param(15, "STRING", "eventId", string = "42")
        param(15, "LONG", "run.id", long = 7)
        param(15, "DATE", "requestTime", date = LocalDateTime.of(2026, 10, 8, 9, 30))
        param(15, "DOUBLE", "rate", double = 0.5)
        param(14, "STRING", "eventId", string = "41")
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("DROP TABLE BATCH_JOB_EXECUTION_PARAMS")
        jdbcTemplate.execute("DROP TABLE BATCH_JOB_EXECUTION")
        jdbcTemplate.execute("DROP TABLE BATCH_JOB_INSTANCE")
    }

    @Test
    fun `실행 이력은 JOB_EXECUTION_ID 내림차순으로 페이지 조회한다`() {
        val first = query.findExecutions(null, PageRequest.of(0, 4))
        assertEquals(listOf(15L, 14L, 13L, 12L), first.content.map { it.executionId })
        assertEquals(6, first.totalElements)
        assertEquals(2, first.totalPages)

        val second = query.findExecutions(null, PageRequest.of(1, 4))
        assertEquals(listOf(11L, 10L), second.content.map { it.executionId })
    }

    @Test
    fun `jobName 은 정확히 일치하는 잡만 거른다 (인스턴스가 여러 개여도 같은 잡)`() {
        val page = query.findExecutions("이벤트정산서", PageRequest.of(0, 20))
        assertEquals(listOf(15L, 14L, 12L), page.content.map { it.executionId })
        assertEquals(3, page.totalElements)
        assertTrue(page.content.all { it.jobName == "이벤트정산서" })

        assertEquals(0, query.findExecutions("이벤트정산", PageRequest.of(0, 20)).totalElements)
    }

    @Test
    fun `실행 컬럼과 TYPE_CD 별 파라미터 값을 문자열로 매핑한다`() {
        val rows = query.findExecutions(null, PageRequest.of(0, 20)).content.associateBy { it.executionId }

        assertEquals(
            mapOf("eventId" to "42", "rate" to "0.5", "requestTime" to "2026-10-08T09:30", "run.id" to "7"),
            rows.getValue(15).parameters,
        )
        assertEquals(mapOf("eventId" to "41"), rows.getValue(14).parameters)
        assertEquals(emptyMap<String, String>(), rows.getValue(11).parameters)

        val failed = rows.getValue(11)
        assertEquals("이벤트_자동만료", failed.jobName)
        assertEquals("FAILED", failed.status)
        assertEquals("FAILED", failed.exitCode)
        assertEquals(at(5, 0), failed.startTime)
        assertEquals(at(5, 2), failed.endTime)
        assertEquals("java.lang.IllegalStateException: boom", failed.exitMessage)
        assertNull(rows.getValue(13).endTime)
    }

    @Test
    fun `잡 요약 - 마지막 실행, 마지막 성공 시작 시각, 기준 시각 이후 실패 수`() {
        val summaries = query.findJobSummaries(failureSince = LocalDateTime.of(2026, 10, 2, 0, 0)).associateBy { it.jobName }
        assertEquals(setOf("이벤트_자동만료", "이벤트정산서"), summaries.keys)

        val expire = summaries.getValue("이벤트_자동만료")
        assertEquals(13L, expire.lastExecutionId)
        assertEquals("STARTED", expire.lastStatus)
        assertEquals(at(6, 0), expire.lastStartTime)
        assertNull(expire.lastEndTime)
        assertEquals(at(1, 0), expire.lastSuccessTime)
        assertEquals(1L, expire.recentFailureCount)

        val settlement = summaries.getValue("이벤트정산서")
        assertEquals(15L, settlement.lastExecutionId)
        assertEquals("COMPLETED", settlement.lastStatus)
        assertEquals(at(8, 1), settlement.lastEndTime)
        assertEquals(at(8, 0), settlement.lastSuccessTime)
        assertEquals(0L, settlement.recentFailureCount)
    }

    @Test
    fun `실행 이력이 없으면 빈 결과`() {
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_PARAMS")
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION")
        assertTrue(query.findJobSummaries(LocalDateTime.of(2026, 10, 2, 0, 0)).isEmpty())
        val page = query.findExecutions(null, PageRequest.of(0, 20))
        assertEquals(0, page.totalElements)
        assertTrue(page.content.isEmpty())
    }

    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    private fun instance(id: Long, jobName: String) {
        jdbcTemplate.update("INSERT INTO BATCH_JOB_INSTANCE VALUES (?, 0, ?, ?)", id, jobName, "key$id")
    }

    private fun execution(id: Long, instanceId: Long, status: String, start: LocalDateTime?, end: LocalDateTime?, exitMessage: String = "") {
        jdbcTemplate.update(
            "INSERT INTO BATCH_JOB_EXECUTION (JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, " +
                "STATUS, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, instanceId, start, start, end, status, if (status == "STARTED") "UNKNOWN" else status, exitMessage, end ?: start,
        )
    }

    private fun param(
        executionId: Long,
        type: String,
        key: String,
        string: String? = null,
        date: LocalDateTime? = null,
        long: Long = 0,
        double: Double = 0.0,
    ) {
        jdbcTemplate.update(
            "INSERT INTO BATCH_JOB_EXECUTION_PARAMS VALUES (?, ?, ?, ?, ?, ?, ?, 'Y')",
            executionId, type, key, string ?: "", date, long, double,
        )
    }
}
