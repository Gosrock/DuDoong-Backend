package band.gosrock.api.v2.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl

@DisplayName("V2PageResponse")
class V2PageResponseTest {

    @Nested
    @DisplayName("Page 변환")
    inner class FromPage {

        @Test
        @DisplayName("전체 개수/페이지 수와 다음 페이지 여부를 담는다")
        fun middlePage() {
            val page: Page<String> = PageImpl(listOf("c", "d"), PageRequest.of(1, 2), 5)

            val result = V2PageResponse.of(page)

            assertEquals(listOf("c", "d"), result.content)
            assertEquals(1, result.page)
            assertEquals(2, result.size)
            assertEquals(5L, result.totalElements)
            assertEquals(3, result.totalPages)
            assertTrue(result.hasNext)
        }

        @Test
        @DisplayName("마지막 페이지는 hasNext=false, size 는 요청 크기를 유지한다")
        fun lastPage() {
            val page: Page<String> = PageImpl(listOf("e"), PageRequest.of(2, 2), 5)

            val result = V2PageResponse.of(page)

            assertEquals(listOf("e"), result.content)
            assertEquals(2, result.size)
            assertFalse(result.hasNext)
        }

        @Test
        @DisplayName("빈 페이지는 totalElements=0, totalPages=0")
        fun emptyPage() {
            val page: Page<String> = PageImpl(emptyList(), PageRequest.of(0, 10), 0)

            val result = V2PageResponse.of(page)

            assertTrue(result.content.isEmpty())
            assertEquals(0L, result.totalElements)
            assertEquals(0, result.totalPages)
            assertFalse(result.hasNext)
        }

        @Test
        @DisplayName("정적 타입이 Slice 여도 실제 Page 면 전체 개수를 담는다")
        fun pageTypedAsSlice() {
            val slice: Slice<String> = PageImpl(listOf("a"), PageRequest.of(0, 1), 3)

            val result = V2PageResponse.of(slice)

            assertEquals(3L, result.totalElements)
            assertEquals(3, result.totalPages)
        }
    }

    @Nested
    @DisplayName("Slice 변환")
    inner class FromSlice {

        @Test
        @DisplayName("count 를 모르므로 totalElements/totalPages 는 null")
        fun slice() {
            val slice: Slice<Int> = SliceImpl(listOf(1, 2, 3), PageRequest.of(0, 3), true)

            val result = V2PageResponse.of(slice)

            assertEquals(listOf(1, 2, 3), result.content)
            assertEquals(0, result.page)
            assertEquals(3, result.size)
            assertNull(result.totalElements)
            assertNull(result.totalPages)
            assertTrue(result.hasNext)
        }

        @Test
        @DisplayName("다음 페이지가 없으면 hasNext=false")
        fun lastSlice() {
            val slice: Slice<Int> = SliceImpl(listOf(4), PageRequest.of(1, 3), false)

            val result = V2PageResponse.of(slice)

            assertEquals(1, result.page)
            assertFalse(result.hasNext)
        }
    }
}
