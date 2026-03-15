package band.gosrock.api.common.slice

import org.springframework.data.domain.Slice

data class SliceResponse<T>(
    val content: List<T>,
    val page: Long,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        @JvmStatic
        fun <T> of(slice: Slice<T>): SliceResponse<T> =
            SliceResponse(
                content = slice.content,
                page = slice.number.toLong(),
                size = slice.numberOfElements,
                hasNext = slice.hasNext(),
            )
    }
}
