package band.gosrock.api.common.page

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNextPage: Boolean,
) {
    companion object {
        @JvmStatic
        fun <T> of(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                page = page.number,
                size = page.numberOfElements,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNextPage = page.hasNext(),
            )
    }
}
