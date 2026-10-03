package band.gosrock.api.v2.event.dto.request

import band.gosrock.common.annotation.DateFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/** 간편 공연 만들기. endAt > startAt 은 도메인에서 검증 (Event_400_2) */
data class V2CreateEventRequest(
    @field:Schema(description = "호스트 id (매니저 이상)", example = "1")
    @field:NotNull
    @field:Positive
    val hostId: Long?,

    @field:Schema(description = "공연 이름 (1~25자)", example = "고스락 제 22회 정기공연")
    @field:NotBlank
    @field:Size(min = 1, max = 25)
    val name: String?,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", example = "2026.12.20 18:00", description = "공연 시작 시각 (현재 이후)")
    @field:NotNull
    @field:Future
    @field:DateFormat
    val startAt: LocalDateTime?,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", example = "2026.12.20 21:20", description = "공연 종료 시각 (시작 이후)")
    @field:NotNull
    @field:DateFormat
    val endAt: LocalDateTime?,

    @field:Schema(description = "티켓 여부. false 면 체크리스트 티켓 항목 면제 (버스킹 등)", example = "true")
    @field:NotNull
    val hasTicket: Boolean?,
)
