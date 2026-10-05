package band.gosrock.api.v2.gift

import band.gosrock.api.v2.gift.usecase.V2GiftUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** G-3 공개 랜딩의 보낸 사람 이름 가림 (#719 리뷰, 기본안) */
@DisplayName("선물 랜딩 이름 가림")
class V2GiftNameMaskTest {

    @Test
    fun `첫 글자 + 가운데 * + 끝 글자, 2자는 첫 글자 + *, 1자는 *`() {
        assertEquals("김*수", V2GiftUseCase.maskName("김철수"))
        assertEquals("김*", V2GiftUseCase.maskName("김수"))
        assertEquals("*", V2GiftUseCase.maskName("김"))
        assertEquals("남**희", V2GiftUseCase.maskName("남궁민희"))
        assertEquals("d*****g", V2GiftUseCase.maskName("dudoong"))
    }
}
