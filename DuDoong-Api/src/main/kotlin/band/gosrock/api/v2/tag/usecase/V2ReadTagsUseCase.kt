package band.gosrock.api.v2.tag.usecase

import band.gosrock.api.v2.tag.dto.V2TagGroupResponse
import band.gosrock.api.v2.tag.dto.V2TagItemResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadTagsUseCase(
    private val tagAdaptor: TagAdaptor,
) {
    /** 분류별 묶음. 태그가 없는 분류는 빠진다 */
    @Transactional(readOnly = true)
    fun execute(): List<V2TagGroupResponse> =
        tagAdaptor.findAllSorted()
            .groupBy { it.category }
            .map { (category, tags) -> V2TagGroupResponse(category, tags.map { V2TagItemResponse(tagId = it.id!!, name = it.name) }) }
}
