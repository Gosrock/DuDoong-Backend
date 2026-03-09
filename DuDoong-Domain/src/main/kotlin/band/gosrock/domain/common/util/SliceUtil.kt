package band.gosrock.domain.common.util

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl

object SliceUtil {
    @JvmStatic
    fun <T> valueOf(contents: List<T>, pageable: Pageable): Slice<T> {
        val hasNext = hasNext(contents, pageable)
        return SliceImpl(if (hasNext) getContent(contents, pageable) else contents, pageable, hasNext)
    }

    private fun <T> hasNext(content: List<T>, pageable: Pageable): Boolean =
        pageable.isPaged && content.size > pageable.pageSize

    private fun <T> getContent(content: List<T>, pageable: Pageable): List<T> =
        content.subList(0, pageable.pageSize)
}
