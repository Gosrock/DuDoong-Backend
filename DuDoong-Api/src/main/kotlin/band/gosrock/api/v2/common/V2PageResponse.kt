package band.gosrock.api.v2.common

import org.springframework.data.domain.Page
import org.springframework.data.domain.Slice

/**
 * v2 공통 페이징 응답.
 *
 * Page / Slice 를 같은 타입으로 내려 클라이언트 파싱을 하나로 통일한다.
 * Slice 는 count 쿼리를 하지 않으므로 [totalElements], [totalPages] 가 null 이다.
 *
 * @property page 0부터 시작하는 페이지 번호
 * @property size 요청한 페이지 크기 (현재 페이지의 실제 원소 수는 content.size)
 */
data class V2PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long?,
    val totalPages: Int?,
    val hasNext: Boolean,
) {
    companion object {
        /** [Page] 이면 전체 개수까지, 그 외 [Slice] 이면 전체 개수 없이 변환한다. */
        fun <T> of(slice: Slice<T>): V2PageResponse<T> {
            val page = slice as? Page<T>
            return V2PageResponse(
                content = slice.content,
                page = slice.number,
                size = slice.size,
                totalElements = page?.totalElements,
                totalPages = page?.totalPages,
                hasNext = slice.hasNext(),
            )
        }
    }
}
