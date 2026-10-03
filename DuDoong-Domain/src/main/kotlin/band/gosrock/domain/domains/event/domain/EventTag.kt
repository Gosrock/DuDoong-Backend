package band.gosrock.domain.domains.event.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 공연 ↔ 태그 (v2). 태그 정보는 tbl_tag 를 tagId 로 조회한다 */
@Entity(name = "tbl_event_tag")
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_event_tag_event_tag", columnNames = ["event_id", "tag_id"])])
class EventTag(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event,

    @Column(name = "tag_id", nullable = false)
    var tagId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_tag_id")
    var id: Long? = null
        protected set
}
