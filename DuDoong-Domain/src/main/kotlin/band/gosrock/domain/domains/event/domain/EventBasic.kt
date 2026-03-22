package band.gosrock.domain.domains.event.domain

import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class EventBasic(
    @Column(length = 25)
    var name: String? = null,

    var startAt: LocalDateTime? = null,

    var runTime: Long? = null,
) {
    fun isUpdated(): Boolean = this.name != null && this.startAt != null && this.runTime != null

    fun endAt(): LocalDateTime? = if (this.runTime == null) null else this.startAt?.plusMinutes(this.runTime!!)
}
