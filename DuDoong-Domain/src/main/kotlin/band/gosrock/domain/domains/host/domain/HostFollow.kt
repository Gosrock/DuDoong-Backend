package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 관심(팔로우) 호스트 (v2) */
@Entity(name = "tbl_host_follow")
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_host_follow_host_user", columnNames = ["host_id", "user_id"])])
class HostFollow(
    @Column(name = "host_id", nullable = false)
    var hostId: Long,

    @Column(name = "user_id", nullable = false)
    var userId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_follow_id")
    var id: Long? = null
        protected set
}
