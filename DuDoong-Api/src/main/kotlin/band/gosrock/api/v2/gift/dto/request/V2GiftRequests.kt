package band.gosrock.api.v2.gift.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/** G-1 선물 링크 생성 / G-8 메모 수정. 메모는 앞뒤 공백 제외 50자까지 (넘으면 Gift_400_9), 빈 값은 메모 없음 */
data class V2GiftMemoRequest(
    @field:Schema(description = "누구에게 보냈는지 메모 (보낸 사람에게만 보임, 선택)", example = "동생")
    val memo: String? = null,
)
