package band.gosrock.domain.domains.event.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.domains.host.domain.HostContactType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

/** 공연 문의처 (v2, 공연당 N개). 유형은 호스트 연락처와 같은 [HostContactType] 을 쓴다. v1 대응 컬럼 없음 */
@Entity(name = "tbl_event_contact")
class EventContact(
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var type: HostContactType,

    @Column(name = "contact_value", length = EventContact.VALUE_MAX_LENGTH, nullable = false)
    var value: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_contact_id")
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event? = null
        protected set

    @Column(nullable = false)
    var sortOrder: Int = 0
        protected set

    fun assignTo(event: Event, sortOrder: Int) {
        this.event = event
        this.sortOrder = sortOrder
    }

    companion object {
        const val VALUE_MAX_LENGTH = 200
    }
}
