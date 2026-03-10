package band.gosrock.domain.domains.ticket_item.repository

import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OptionGroupRepository : JpaRepository<OptionGroup, Long> {

    fun findByIdAndOptionGroupStatus(optionGroupId: Long, status: OptionGroupStatus): Optional<OptionGroup>

    fun findAllByEventIdAndOptionGroupStatus(eventId: Long, status: OptionGroupStatus): List<OptionGroup>
}
