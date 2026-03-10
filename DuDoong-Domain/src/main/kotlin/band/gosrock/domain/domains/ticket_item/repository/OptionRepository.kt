package band.gosrock.domain.domains.ticket_item.repository

import band.gosrock.domain.domains.ticket_item.domain.Option
import org.springframework.data.jpa.repository.JpaRepository

interface OptionRepository : JpaRepository<Option, Long> {

    fun findAllByIdIn(ids: List<Long>): List<Option>
}
