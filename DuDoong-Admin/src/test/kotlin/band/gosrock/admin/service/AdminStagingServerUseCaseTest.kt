package band.gosrock.admin.service

import band.gosrock.admin.exception.AdminErrorCode
import band.gosrock.admin.model.dto.response.AdminStagingServerResponse
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.infrastructure.outer.aws.StagingAppHealthChecker
import band.gosrock.infrastructure.outer.aws.StagingServerControlException
import band.gosrock.infrastructure.outer.aws.StagingServerClient
import band.gosrock.infrastructure.outer.aws.StagingServerInfo
import band.gosrock.infrastructure.outer.aws.StagingServerState
import java.time.Instant
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
@DisplayName("스테이징 서버 제어 UseCase")
class AdminStagingServerUseCaseTest {

    @Mock
    private lateinit var userAdaptor: UserAdaptor

    @Mock
    private lateinit var stagingServerClient: StagingServerClient

    @Mock
    private lateinit var stagingAppHealthChecker: StagingAppHealthChecker

    private lateinit var getUseCase: AdminGetStagingServerUseCase
    private lateinit var startUseCase: AdminStartStagingServerUseCase
    private lateinit var stopUseCase: AdminStopStagingServerUseCase

    private val launchTime: Instant = Instant.parse("2026-10-09T01:00:00Z")

    @BeforeEach
    fun setUp() {
        val adminAuthValidator = AdminAuthValidator(userAdaptor)
        getUseCase = AdminGetStagingServerUseCase(adminAuthValidator, stagingServerClient, stagingAppHealthChecker)
        startUseCase = AdminStartStagingServerUseCase(adminAuthValidator, stagingServerClient)
        stopUseCase = AdminStopStagingServerUseCase(adminAuthValidator, stagingServerClient)
    }

    private fun givenUser(userId: Long, role: AccountRole) {
        val user = User()
        ReflectionTestUtils.setField(user, "id", userId)
        ReflectionTestUtils.setField(user, "accountRole", role)
        `when`(userAdaptor.queryUser(userId)).thenReturn(user)
    }

    private fun info(state: StagingServerState, launch: Instant? = launchTime) = StagingServerInfo(state, launch)

    @Nested
    @DisplayName("조회")
    inner class GetTest {

        @Test
        @DisplayName("RUNNING 이면 KST 기준 launchedAt 과 url 을 내려준다")
        fun running() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.RUNNING))

            val response = getUseCase.execute(1L)

            assertEquals("RUNNING", response.state)
            assertEquals(LocalDateTime.of(2026, 10, 9, 10, 0), response.launchedAt)
            assertEquals("https://staging.dudoong.com", response.url)
        }

        @Test
        @DisplayName("STOPPED 이면 launchedAt 은 null 이다")
        fun stopped() {
            givenUser(1L, AccountRole.SUPER_ADMIN)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.STOPPED))

            val response = getUseCase.execute(1L)

            assertEquals("STOPPED", response.state)
            assertNull(response.launchedAt)
        }

        @Test
        @DisplayName("설정이 없으면 NOT_CONFIGURED 를 내려준다")
        fun notConfigured() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.NOT_CONFIGURED, null))

            val response = getUseCase.execute(1L)

            assertEquals("NOT_CONFIGURED", response.state)
            assertNull(response.launchedAt)
        }

        @Test
        @DisplayName("MANAGER 는 조회할 수 없다")
        fun managerDenied() {
            givenUser(1L, AccountRole.MANAGER)

            assertThrows(DuDoongCodeException::class.java) { getUseCase.execute(1L) }
            verifyNoInteractions(stagingServerClient)
        }
    }

    @Nested
    @DisplayName("시작")
    inner class StartTest {

        @Test
        @DisplayName("STOPPED 이면 start 를 호출하고, 다시 조회하지 않고 start 응답 상태(PENDING)를 내려준다")
        fun startFromStopped() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.STOPPED))
            `when`(stagingServerClient.start()).thenReturn(StagingServerState.PENDING)

            val response = startUseCase.execute(1L)

            verify(stagingServerClient).start()
            verify(stagingServerClient, times(1)).describe()
            assertEquals("PENDING", response.state)
            assertNull(response.launchedAt)
        }

        @Test
        @DisplayName("이미 RUNNING 이면 start 를 호출하지 않는다")
        fun alreadyRunning() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.RUNNING))

            val response = startUseCase.execute(1L)

            verify(stagingServerClient, never()).start()
            assertEquals("RUNNING", response.state)
        }

        @Test
        @DisplayName("이미 PENDING 이면 start 를 호출하지 않는다")
        fun alreadyPending() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.PENDING))

            val response = startUseCase.execute(1L)

            verify(stagingServerClient, never()).start()
            assertEquals("PENDING", response.state)
        }

        @Test
        @DisplayName("설정이 없으면 STAGING_SERVER_NOT_CONFIGURED 예외")
        fun notConfigured() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(false)

            val exception = assertThrows(DuDoongCodeException::class.java) { startUseCase.execute(1L) }

            assertEquals(AdminErrorCode.STAGING_SERVER_NOT_CONFIGURED, exception.errorCode)
            assertEquals(400, exception.getErrorReason().status)
            verify(stagingServerClient, never()).start()
        }

        @Test
        @DisplayName("USER 는 시작할 수 없다")
        fun userDenied() {
            givenUser(1L, AccountRole.USER)

            assertThrows(DuDoongCodeException::class.java) { startUseCase.execute(1L) }
            verifyNoInteractions(stagingServerClient)
        }
    }

    @Nested
    @DisplayName("중지")
    inner class StopTest {

        @Test
        @DisplayName("RUNNING 이면 stop 을 호출하고, 다시 조회하지 않고 stop 응답 상태(STOPPING)를 내려준다")
        fun stopFromRunning() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.RUNNING))
            `when`(stagingServerClient.stop()).thenReturn(StagingServerState.STOPPING)

            val response = stopUseCase.execute(1L)

            verify(stagingServerClient).stop()
            verify(stagingServerClient, times(1)).describe()
            assertEquals("STOPPING", response.state)
            assertNull(response.launchedAt)
        }

        @Test
        @DisplayName("이미 STOPPED 이면 stop 을 호출하지 않는다")
        fun alreadyStopped() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.STOPPED))

            val response = stopUseCase.execute(1L)

            verify(stagingServerClient, never()).stop()
            assertEquals("STOPPED", response.state)
        }

        @Test
        @DisplayName("이미 STOPPING 이면 stop 을 호출하지 않는다")
        fun alreadyStopping() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.STOPPING))

            stopUseCase.execute(1L)

            verify(stagingServerClient, never()).stop()
        }

        @Test
        @DisplayName("설정이 없으면 STAGING_SERVER_NOT_CONFIGURED 예외")
        fun notConfigured() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(false)

            val exception = assertThrows(DuDoongCodeException::class.java) { stopUseCase.execute(1L) }

            assertEquals(AdminErrorCode.STAGING_SERVER_NOT_CONFIGURED, exception.errorCode)
            verify(stagingServerClient, never()).stop()
        }

        @Test
        @DisplayName("MANAGER 는 중지할 수 없다")
        fun managerDenied() {
            givenUser(1L, AccountRole.MANAGER)

            assertThrows(DuDoongCodeException::class.java) { stopUseCase.execute(1L) }
            verifyNoInteractions(stagingServerClient)
        }
    }

    @Nested
    @DisplayName("nextAutoStopAt")
    inner class NextAutoStopAtTest {

        @Test
        @DisplayName("01:59 이면 오늘 02:00")
        fun beforeTwo() {
            assertEquals(
                LocalDateTime.of(2026, 10, 9, 2, 0),
                AdminStagingServerResponse.nextAutoStopAt(LocalDateTime.of(2026, 10, 9, 1, 59)),
            )
        }

        @Test
        @DisplayName("정확히 02:00 이면 내일 02:00")
        fun exactlyTwo() {
            assertEquals(
                LocalDateTime.of(2026, 10, 10, 2, 0),
                AdminStagingServerResponse.nextAutoStopAt(LocalDateTime.of(2026, 10, 9, 2, 0)),
            )
        }

        @Test
        @DisplayName("15:00 이면 내일 02:00")
        fun afternoon() {
            assertEquals(
                LocalDateTime.of(2026, 10, 10, 2, 0),
                AdminStagingServerResponse.nextAutoStopAt(LocalDateTime.of(2026, 10, 9, 15, 0)),
            )
        }

        @Test
        @DisplayName("월말 23:30 이면 다음 달 1일 02:00")
        fun endOfMonth() {
            assertEquals(
                LocalDateTime.of(2026, 11, 1, 2, 0),
                AdminStagingServerResponse.nextAutoStopAt(LocalDateTime.of(2026, 10, 31, 23, 30)),
            )
        }
    }

    @Nested
    @DisplayName("앱 상태 (조회)")
    inner class AppStatusTest {

        private val privateIp = "10.0.0.10"

        @Test
        @DisplayName("RUNNING 이고 헬스체크 200 이면 UP")
        fun up() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe())
                .thenReturn(StagingServerInfo(StagingServerState.RUNNING, launchTime, privateIp))
            `when`(stagingAppHealthChecker.isHealthy(privateIp)).thenReturn(true)

            assertEquals("UP", getUseCase.execute(1L).appStatus)
        }

        @Test
        @DisplayName("RUNNING 이고 헬스체크 실패여도 켠 지 10분 안이면 STARTING")
        fun starting() {
            givenUser(1L, AccountRole.ADMIN)
            val justLaunched = Instant.now().minusSeconds(60)
            `when`(stagingServerClient.describe())
                .thenReturn(StagingServerInfo(StagingServerState.RUNNING, justLaunched, privateIp))
            `when`(stagingAppHealthChecker.isHealthy(privateIp)).thenReturn(false)

            assertEquals("STARTING", getUseCase.execute(1L).appStatus)
        }

        @Test
        @DisplayName("RUNNING 이고 켠 지 10분이 지나도 헬스체크 실패면 DOWN")
        fun down() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe())
                .thenReturn(StagingServerInfo(StagingServerState.RUNNING, launchTime, privateIp))
            `when`(stagingAppHealthChecker.isHealthy(privateIp)).thenReturn(false)

            assertEquals("DOWN", getUseCase.execute(1L).appStatus)
        }

        @Test
        @DisplayName("사설 IP 를 모르면 헬스체크 없이 판단한다")
        fun noPrivateIp() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.RUNNING))

            assertEquals("DOWN", getUseCase.execute(1L).appStatus)
            verifyNoInteractions(stagingAppHealthChecker)
        }

        @Test
        @DisplayName("RUNNING 이 아니면 appStatus 는 null 이고 헬스체크를 하지 않는다")
        fun notRunning() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe())
                .thenReturn(StagingServerInfo(StagingServerState.PENDING, launchTime, privateIp))

            assertNull(getUseCase.execute(1L).appStatus)
            verifyNoInteractions(stagingAppHealthChecker)
        }

        @Test
        @DisplayName("appStatusOf 경계: 9분 59초는 STARTING, 10분은 DOWN, 켜진 시각 없으면 DOWN")
        fun appStatusOfBoundary() {
            val launched = LocalDateTime.of(2026, 10, 9, 10, 0)
            assertEquals("STARTING", AdminStagingServerResponse.appStatusOf(false, launched, launched.plusMinutes(9).plusSeconds(59)))
            assertEquals("DOWN", AdminStagingServerResponse.appStatusOf(false, launched, launched.plusMinutes(10)))
            assertEquals("DOWN", AdminStagingServerResponse.appStatusOf(false, null, launched))
            assertEquals("UP", AdminStagingServerResponse.appStatusOf(true, null, launched))
        }
    }

    @Nested
    @DisplayName("AWS 오류")
    inner class AwsErrorTest {

        @Test
        @DisplayName("끄는 중에 켜기처럼 상태 충돌이면 409 (ADMIN_409_1)")
        fun stateConflict() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.isConfigured()).thenReturn(true)
            `when`(stagingServerClient.describe()).thenReturn(info(StagingServerState.STOPPED))
            `when`(stagingServerClient.start()).thenThrow(
                StagingServerControlException(StagingServerControlException.Kind.STATE_CONFLICT, "IncorrectInstanceState"),
            )

            val exception = assertThrows(DuDoongCodeException::class.java) { startUseCase.execute(1L) }

            assertEquals(AdminErrorCode.STAGING_SERVER_STATE_CONFLICT, exception.errorCode)
            assertEquals(409, exception.getErrorReason().status)
        }

        @Test
        @DisplayName("권한·네트워크 문제면 조회도 503 (ADMIN_503_1)")
        fun unavailable() {
            givenUser(1L, AccountRole.ADMIN)
            `when`(stagingServerClient.describe()).thenThrow(
                StagingServerControlException(StagingServerControlException.Kind.UNAVAILABLE, "UnauthorizedOperation"),
            )

            val exception = assertThrows(DuDoongCodeException::class.java) { getUseCase.execute(1L) }

            assertEquals(AdminErrorCode.STAGING_SERVER_UNAVAILABLE, exception.errorCode)
            assertEquals(503, exception.getErrorReason().status)
        }
    }
}
