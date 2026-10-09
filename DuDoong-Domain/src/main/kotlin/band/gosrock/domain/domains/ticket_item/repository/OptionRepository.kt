package band.gosrock.domain.domains.ticket_item.repository

import band.gosrock.domain.domains.ticket_item.domain.Option
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OptionRepository : JpaRepository<Option, Long> {

    fun findAllByIdIn(ids: List<Long>): List<Option>

    /** 옵션 행 + 옵션 그룹(질문 이름·설명)을 쿼리 1개로 (v2 옵션 답변 응답, #752) */
    @Query("select o from tbl_option o left join fetch o.optionGroup where o.id in :ids")
    fun findAllWithGroupByIdIn(@Param("ids") ids: List<Long>): List<Option>
}
