package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.host.HostUserJoinEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
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
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity(name = "tbl_host_user")
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["host_id", "user_id"])])
class HostUser(
    // 소속 호스트를 관리중인 유저 아이디
    @Column(name = "user_id")
    var userId: Long? = null,

    // 유저의 권한
    @Enumerated(EnumType.STRING)
    var role: HostRole = HostRole.GUEST,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_user_id")
    var id: Long? = null
        protected set

    // 소속 호스트 아이디
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "host_id")
    var host: Host? = null
        protected set

    // 초대 승락 여부
    var active: Boolean = false
        protected set

    constructor(host: Host, userId: Long?, role: HostRole) : this(userId = userId, role = role) {
        this.host = host
    }

    fun setHostRole(role: HostRole) {
        this.role = role
    }

    fun activate() {
        if (this.active) throw AlreadyJoinedHostException.EXCEPTION
        this.active = true
        Events.raise(HostUserJoinEvent.of(this.host!!.id, this.userId))
    }
}
