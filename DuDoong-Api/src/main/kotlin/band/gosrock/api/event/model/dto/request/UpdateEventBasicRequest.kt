package band.gosrock.api.event.model.dto.request

import band.gosrock.common.annotation.DateFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.hibernate.validator.constraints.Length

data class UpdateEventBasicRequest(
    @field:Schema(defaultValue = "고스락 제 22회 정기공연", description = "공연 이름")
    @field:NotBlank(message = "공연 이름을 입력하세요")
    @field:Length(max = 25)
    val name: String? = null,

    @field:Schema(
        type = "string",
        pattern = "yyyy.MM.dd HH:mm",
        defaultValue = "2023.03.20 12:00",
        description = "공연 시작 시각"
    )
    @field:NotNull(message = "공연 시작 시각을 입력하세요")
    @field:Future(message = "공연 시작 시각은 현재보다 이후여야 합니다.")
    @field:DateFormat
    val startAt: LocalDateTime? = null,

    @field:Schema(defaultValue = "90", description = "공연 진행시간")
    @field:Positive(message = "공연 진행 예상 소요시간(분)을 입력하세요")
    val runTime: Long? = null,

    // 공연 장소
    @field:Schema(defaultValue = "롤링홀", description = "공연장 이름")
    @field:NotBlank(message = "공연장 이름을 입력하세요")
    val placeName: String? = null,

    // 공연 상세 주소
    @field:Schema(defaultValue = "서울 마포구 어울마당로 35", description = "공연장 주소")
    @field:NotBlank(message = "공연장 상세주소를 입력하세요")
    val placeAddress: String? = null,

    // (지도 정보) 경도 - x
    @field:Schema(defaultValue = "126.920036", description = "공연장 위치 경도")
    @field:Positive(message = "공연장 경도 정보를 입력하세요")
    val longitude: Double? = null,

    // (지도 정보) 위도 - y
    @field:Schema(defaultValue = "37.548369", description = "공연장 위치 위도")
    @field:Positive(message = "공연장 위도 정보를 입력하세요")
    val latitude: Double? = null
)
