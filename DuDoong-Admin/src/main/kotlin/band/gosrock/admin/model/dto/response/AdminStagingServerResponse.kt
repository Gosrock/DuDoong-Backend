package band.gosrock.admin.model.dto.response

import band.gosrock.infrastructure.outer.aws.StagingServerInfo
import band.gosrock.infrastructure.outer.aws.StagingServerState
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class AdminStagingServerResponse(
    val state: String,
    val launchedAt: LocalDateTime?,
    val nextAutoStopAt: LocalDateTime,
    val url: String,
) {
    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private const val STAGING_URL = "https://staging.dudoong.com"
        private val AUTO_STOP_TIME: LocalTime = LocalTime.of(2, 0)

        fun of(info: StagingServerInfo, now: LocalDateTime): AdminStagingServerResponse {
            val launched = info.state == StagingServerState.PENDING || info.state == StagingServerState.RUNNING
            return AdminStagingServerResponse(
                state = info.state.name,
                launchedAt = if (launched) info.launchTime?.atZone(KST)?.toLocalDateTime() else null,
                nextAutoStopAt = nextAutoStopAt(now),
                url = STAGING_URL,
            )
        }

        /** 매일 02:00(KST) 자동 중지 기준으로, now 보다 엄격히 이후인 가장 가까운 02:00 을 돌려준다. */
        fun nextAutoStopAt(now: LocalDateTime): LocalDateTime {
            val todayStop = now.toLocalDate().atTime(AUTO_STOP_TIME)
            return if (now.isBefore(todayStop)) todayStop else todayStop.plusDays(1)
        }
    }
}
