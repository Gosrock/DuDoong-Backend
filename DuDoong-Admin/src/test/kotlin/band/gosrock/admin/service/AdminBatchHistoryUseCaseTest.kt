package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminBatchExecutionResponse
import band.gosrock.admin.repository.AdminBatchExecutionQuery
import band.gosrock.admin.repository.BatchExecutionRow
import band.gosrock.admin.repository.BatchJobSummaryRow
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
@DisplayName("배치 실행 이력 UseCase")
class AdminBatchHistoryUseCaseTest {

    @Mock
    private lateinit var userAdaptor: UserAdaptor

    @Mock
    private lateinit var adminBatchExecutionQuery: AdminBatchExecutionQuery

    private lateinit var adminGetBatchJobsUseCase: AdminGetBatchJobsUseCase
    private lateinit var adminGetBatchExecutionsUseCase: AdminGetBatchExecutionsUseCase

    @BeforeEach
    fun setUp() {
        val adminAuthValidator = AdminAuthValidator(userAdaptor)
        adminGetBatchJobsUseCase = AdminGetBatchJobsUseCase(adminBatchExecutionQuery, adminAuthValidator)
        adminGetBatchExecutionsUseCase = AdminGetBatchExecutionsUseCase(adminBatchExecutionQuery, adminAuthValidator)
    }

    /** Kotlin non-null 파라미터에 ArgumentCaptor 를 넘기기 위한 래퍼. 매처만 등록하고, 호출부 null 검사를 피하려 [placeholder] 를 돌려준다 */
    private fun <T> capture(captor: ArgumentCaptor<T>, placeholder: T): T {
        captor.capture()
        return placeholder
    }

    private fun givenUser(role: AccountRole) {
        val user = User(profile = Profile(name = "관리자"))
        ReflectionTestUtils.setField(user, "id", 1L)
        ReflectionTestUtils.setField(user, "accountRole", role)
        `when`(userAdaptor.queryUser(1L)).thenReturn(user)
    }

    private fun summary(jobName: String, lastStartTime: LocalDateTime?) = BatchJobSummaryRow(
        jobName = jobName,
        lastExecutionId = 1L,
        lastStatus = "COMPLETED",
        lastStartTime = lastStartTime,
        lastEndTime = lastStartTime?.plusMinutes(1),
        lastSuccessTime = lastStartTime,
        recentFailureCount = 2L,
    )

    private fun execution(start: LocalDateTime?, end: LocalDateTime?, exitMessage: String?) = BatchExecutionRow(
        executionId = 100L,
        jobName = "이벤트정산서",
        status = "FAILED",
        exitCode = "FAILED",
        startTime = start,
        endTime = end,
        exitMessage = exitMessage,
        parameters = mapOf("eventId" to "42"),
    )

    @Nested
    @DisplayName("잡 요약 조회")
    inner class JobsTest {

        @Test
        @DisplayName("USER 역할이면 예외가 발생하고 조회하지 않는다")
        fun userRoleDenied() {
            givenUser(AccountRole.USER)
            assertThrows(DuDoongCodeException::class.java) { adminGetBatchJobsUseCase.execute(1L) }
            verifyNoInteractions(adminBatchExecutionQuery)
        }

        @Test
        @DisplayName("마지막 시작 시각 내림차순(없으면 맨 뒤)으로 매핑하고, 실패 집계 기준은 KST 7일 전이다")
        fun mapsAndSorts() {
            givenUser(AccountRole.ADMIN)
            val base = LocalDateTime.of(2026, 10, 8, 3, 0)
            val captor = ArgumentCaptor.forClass(LocalDateTime::class.java)
            `when`(adminBatchExecutionQuery.findJobSummaries(capture(captor, LocalDateTime.MIN))).thenReturn(
                listOf(summary("슬랙유저통계", base.minusDays(1)), summary("미실행", null), summary("이벤트_자동만료", base)),
            )

            val result = adminGetBatchJobsUseCase.execute(1L)

            assertEquals(listOf("이벤트_자동만료", "슬랙유저통계", "미실행"), result.map { it.jobName })
            val first = result.first()
            assertEquals(1L, first.lastExecutionId)
            assertEquals("COMPLETED", first.lastStatus)
            assertEquals(base, first.lastStartTime)
            assertEquals(base.plusMinutes(1), first.lastEndTime)
            assertEquals(base, first.lastSuccessTime)
            assertEquals(2L, first.recentFailureCount)

            val expectedSince = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(7)
            assertTrue(Duration.between(captor.value, expectedSince).abs() < Duration.ofMinutes(1))
        }
    }

    @Nested
    @DisplayName("실행 이력 조회")
    inner class ExecutionsTest {

        @Test
        @DisplayName("USER 역할이면 예외가 발생하고 조회하지 않는다")
        fun userRoleDenied() {
            givenUser(AccountRole.USER)
            assertThrows(DuDoongCodeException::class.java) {
                adminGetBatchExecutionsUseCase.execute(1L, null, PageRequest.of(0, 20))
            }
            verifyNoInteractions(adminBatchExecutionQuery)
        }

        @Test
        @DisplayName("jobName 과 페이지를 넘기고, size 는 100 으로 제한한다")
        fun capsPageSize() {
            givenUser(AccountRole.SUPER_ADMIN)
            val pageCaptor = ArgumentCaptor.forClass(Pageable::class.java)
            val jobCaptor = ArgumentCaptor.forClass(String::class.java)
            `when`(adminBatchExecutionQuery.findExecutions(capture(jobCaptor, ""), capture(pageCaptor, Pageable.unpaged())))
                .thenReturn(PageImpl(emptyList(), PageRequest.of(2, 100), 0))

            adminGetBatchExecutionsUseCase.execute(1L, "이벤트정산서", PageRequest.of(2, 500))

            assertEquals("이벤트정산서", jobCaptor.value)
            assertEquals(2, pageCaptor.value.pageNumber)
            assertEquals(100, pageCaptor.value.pageSize)
        }

        @Test
        @DisplayName("빈 jobName 은 필터 없이 조회하고, 100 이하 size 는 그대로 쓴다")
        fun blankJobName() {
            givenUser(AccountRole.ADMIN)
            val pageCaptor = ArgumentCaptor.forClass(Pageable::class.java)
            val jobCaptor = ArgumentCaptor.forClass(String::class.java)
            `when`(adminBatchExecutionQuery.findExecutions(capture(jobCaptor, ""), capture(pageCaptor, Pageable.unpaged())))
                .thenReturn(PageImpl(emptyList(), PageRequest.of(0, 20), 0))

            adminGetBatchExecutionsUseCase.execute(1L, " ", PageRequest.of(0, 20))

            assertNull(jobCaptor.value)
            assertEquals(20, pageCaptor.value.pageSize)
        }

        @Test
        @DisplayName("소요 시간을 초로 계산하고, 종료 메시지는 첫 줄만 200자로 자른다")
        fun mapsDurationAndTruncates() {
            givenUser(AccountRole.ADMIN)
            val start = LocalDateTime.of(2026, 10, 8, 3, 0, 0)
            val pageable = PageRequest.of(0, 20)
            `when`(adminBatchExecutionQuery.findExecutions(null, pageable)).thenReturn(
                PageImpl(listOf(execution(start, start.plusSeconds(95), "x".repeat(2000) + "\n\tat secret.Stack(Trace.java:1)")), pageable, 41),
            )

            val page = adminGetBatchExecutionsUseCase.execute(1L, null, pageable)

            assertEquals(41, page.totalElements)
            val item = page.content.single()
            assertEquals(100L, item.executionId)
            assertEquals("이벤트정산서", item.jobName)
            assertEquals("FAILED", item.status)
            assertEquals("FAILED", item.exitCode)
            assertEquals(95.0, item.durationSeconds)
            assertEquals(200, item.exitMessage?.length)
            assertTrue(item.exitMessage!!.none { it == '\n' })
            assertEquals(mapOf("eventId" to "42"), item.parameters)
        }

        @Test
        @DisplayName("시작/종료 시각이 없으면 소요 시간은 null, 빈 종료 메시지는 null")
        fun nullDurationAndBlankMessage() {
            givenUser(AccountRole.ADMIN)
            val start = LocalDateTime.of(2026, 10, 8, 3, 0, 0)
            val pageable = PageRequest.of(0, 20)
            `when`(adminBatchExecutionQuery.findExecutions(null, pageable)).thenReturn(
                PageImpl(listOf(execution(start, null, ""), execution(null, start, "  ")), pageable, 2),
            )

            val items = adminGetBatchExecutionsUseCase.execute(1L, null, pageable).content

            assertTrue(items.all { it.durationSeconds == null && it.exitMessage == null })
        }
    }
    @Nested
    @DisplayName("소요 시간")
    inner class DurationTest {

        @Test
        @DisplayName("밀리초를 소수 첫째 자리 초로 반올림한다 (1.743초 → 1.7)")
        fun roundsToOneDecimal() {
            val start = LocalDateTime.of(2026, 10, 9, 19, 41, 35, 477_000_000)
            assertEquals(1.7, AdminBatchExecutionResponse.durationSecondsOf(start, start.plusNanos(1_743_000_000)))
            assertEquals(0.1, AdminBatchExecutionResponse.durationSecondsOf(start, start.plusNanos(50_000_000)))
            assertEquals(0.0, AdminBatchExecutionResponse.durationSecondsOf(start, start))
            assertNull(AdminBatchExecutionResponse.durationSecondsOf(start, null))
        }
    }

    @Nested
    @DisplayName("종료 메시지 요약")
    inner class ExitMessageTest {

        @Test
        @DisplayName("스택트레이스는 버리고 첫 줄(예외와 메시지)만 남긴다")
        fun firstLineOnly() {
            val message = "\n  java.sql.SQLException: Access denied for user 'x'@'host'\n\tat com.mysql.Driver(Driver.java:10)\nCaused by: ..."
            assertEquals(
                "java.sql.SQLException: Access denied for user 'x'@'host'",
                AdminBatchExecutionResponse.summarizeExitMessage(message),
            )
            assertNull(AdminBatchExecutionResponse.summarizeExitMessage("   \n  "))
            assertNull(AdminBatchExecutionResponse.summarizeExitMessage(null))
        }
    }
}
