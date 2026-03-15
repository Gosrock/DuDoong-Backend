package band.gosrock.api.event.model.dto.request

import band.gosrock.common.annotation.DateFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import javax.validation.constraints.Future
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import org.hibernate.validator.constraints.Length

data class CreateEventRequest(
    @field:Schema(defaultValue = "1", description = "호스트 고유 아이디")
    @field:Positive
    val hostId: Long? = null,

    @field:Schema(defaultValue = "고스락 제 22회 정기공연", description = "공연 이름")
    @field:NotBlank(message = "공연 이름을 입력하세요.")
    @field:Length(max = 25)
    val name: String? = null,

    @field:Schema(
        type = "string",
        pattern = "yyyy.MM.dd HH:mm",
        defaultValue = "2023.03.20 12:00",
        description = "공연 시작 시각"
    )
    @field:NotNull(message = "공연 시작 시각을 입력하세요.")
    @field:Future(message = "공연 시작 시각은 현재보다 이후여야 합니다.")
    @field:DateFormat
    val startAt: LocalDateTime? = null,

    @field:Schema(defaultValue = "90", description = "공연 진행시간")
    @field:Positive(message = "공연 진행 예상 소요시간(분)을 입력하세요.")
    val runTime: Long? = null
)
