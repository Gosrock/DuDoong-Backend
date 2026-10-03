package band.gosrock.domain.domains.tag.repository

import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.domain.TagCategory
import org.springframework.data.repository.CrudRepository

interface TagRepository : CrudRepository<Tag, Long> {
    override fun findAll(): List<Tag>

    fun findAllByIdIn(ids: Collection<Long>): List<Tag>

    fun existsByCategoryAndName(category: TagCategory, name: String): Boolean
}
