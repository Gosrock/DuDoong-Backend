package band.gosrock.api.v2.tag.dto

import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.domain.TagCategory

data class V2TagResponse(
    val tagId: Long,
    val category: TagCategory,
    val name: String,
) {
    companion object {
        fun from(tag: Tag): V2TagResponse = V2TagResponse(tagId = tag.id!!, category = tag.category, name = tag.name)
    }
}

/** 분류별 태그 묶음 (E-11) */
data class V2TagGroupResponse(
    val category: TagCategory,
    val tags: List<V2TagItemResponse>,
)

data class V2TagItemResponse(
    val tagId: Long,
    val name: String,
)
