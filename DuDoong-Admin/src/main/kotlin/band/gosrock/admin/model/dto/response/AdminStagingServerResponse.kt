package band.gosrock.admin.model.dto.response

import band.gosrock.infrastructure.outer.aws.StagingServerInfo
import band.gosrock.infrastructure.outer.aws.StagingServerState
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class AdminStagingServerResponse(
    val state: String,
    val launchedAt: LocalDateTime?,
    val nextAutoStopAt: LocalDateTime,
    val url: String,
    /** 앱 상태. 서버가 RUNNING 일 때만 UP / STARTING / DOWN, 그 밖에는 null */
    val appStatus: String?,
) {
    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val STAGING_URL = "https://staging.dudoong.com"
        private val AUTO_STOP_TIME: LocalTime = LocalTime.of(2, 0)

        /** 켠 뒤 이 시간 안에 헬스체크가 실패하면 아직 뜨는 중(STARTING)으로 본다. t3.micro 에서 컨테이너 6개 기동을 고려 */
        val APP_STARTUP_GRACE: Duration = Duration.ofMinutes(10)

        fun of(info: StagingServerInfo, now: LocalDateTime, appStatus: String? = null): AdminStagingServerResponse {
            val launched = info.state == StagingServerState.PENDING || info.state == StagingServerState.RUNNING
            return AdminStagingServerResponse(
                state = info.state.name,
                launchedAt = if (launched) info.launchTime?.atZone(KST)?.toLocalDateTime() else null,
                nextAutoStopAt = nextAutoStopAt(now),
                url = STAGING_URL,
                appStatus = appStatus,
            )
        }

        /** 매일 02:00(KST) 자동 중지 기준으로, now 보다 엄격히 이후인 가장 가까운 02:00 을 돌려준다. */
        fun nextAutoStopAt(now: LocalDateTime): LocalDateTime {
            val todayStop = now.toLocalDate().atTime(AUTO_STOP_TIME)
            return if (now.isBefore(todayStop)) todayStop else todayStop.plusDays(1)
        }

        /**
         * 헬스체크 결과로 앱 상태를 정한다.
         * 응답 200 → UP, 실패해도 켠 지 10분 안이면 STARTING, 그 뒤로도 실패면 DOWN.
         */
        fun appStatusOf(healthy: Boolean, launchedAt: LocalDateTime?, now: LocalDateTime): String =
            when {
                healthy -> "UP"
                launchedAt != null && Duration.between(launchedAt, now) < APP_STARTUP_GRACE -> "STARTING"
                else -> "DOWN"
            }
    }
}
