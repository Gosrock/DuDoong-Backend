package band.gosrock.api.common.slice

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import javax.validation.constraints.Positive

@Deprecated("Use cursor-based pagination instead")
class SliceParam(
    @field:Schema(description = "현재 조회하려는 페이지")
    @field:Positive
    var page: Int? = null,

    @field:Schema(description = "한 번 조회당 레코드 개수 (default: 10)")
    @field:Positive
    var size: Int? = null,

    @field:Schema(description = "정렬 조건 프로퍼티 (default: \"id\")")
    var sort: String? = null,

    @field:Schema(description = "정렬 순서 (ASC | DESC) (default: DESC)")
    var direction: Sort.Direction? = null,
) {
    fun toPageable(): Pageable {
        // RequestParam 에 빈 값은 null 로 할당되어 검증 필요
        if (page == null) page = 0
        if (size == null) size = 10
        if (sort == null) sort = "id"
        if (direction == null) direction = Sort.Direction.DESC
        return PageRequest.of(page!!, size!!, Sort.by(direction!!, sort!!))
    }

    companion object {
        @JvmStatic
        fun pageableOf(sliceParam: SliceParam): Pageable = sliceParam.toPageable()
    }
}
