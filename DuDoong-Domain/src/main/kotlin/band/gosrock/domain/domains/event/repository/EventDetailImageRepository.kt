package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.domains.event.domain.EventDetailImage
import org.springframework.data.repository.CrudRepository

interface EventDetailImageRepository : CrudRepository<EventDetailImage, Long>
