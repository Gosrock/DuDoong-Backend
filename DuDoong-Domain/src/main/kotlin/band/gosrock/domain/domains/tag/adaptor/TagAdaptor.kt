package band.gosrock.domain.domains.tag.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.repository.TagRepository

@Adaptor
class TagAdaptor(private val tagRepository: TagRepository) {

    /** 분류 순서(enum 선언 순) → 분류 안 sortOrder → id 순 */
    fun findAllSorted(): List<Tag> =
        tagRepository.findAll().sortedWith(compareBy({ it.category.ordinal }, { it.sortOrder }, { it.id }))

    fun findAllByIdIn(ids: Collection<Long>): List<Tag> =
        if (ids.isEmpty()) emptyList() else tagRepository.findAllByIdIn(ids)
}
