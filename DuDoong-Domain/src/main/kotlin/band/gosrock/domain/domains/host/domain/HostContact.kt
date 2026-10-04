package band.gosrock.domain.domains.host.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.HostContactVo
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

/** 호스트 대표 연락처 (v2, 호스트당 N개). 순서는 [sortOrder] 오름차순 */
@Entity(name = "tbl_host_contact")
class HostContact(
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var type: HostContactType,

    @Column(name = "contact_value", length = HostContact.VALUE_MAX_LENGTH, nullable = false)
    var value: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_contact_id")
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    var host: Host? = null
        protected set

    @Column(nullable = false)
    var sortOrder: Int = 0
        protected set

    fun assignTo(host: Host, sortOrder: Int) {
        this.host = host
        this.sortOrder = sortOrder
    }

    fun changeValue(value: String) {
        this.value = value
    }

    fun toHostContactVo(): HostContactVo = HostContactVo(type = type, value = value)

    companion object {
        const val VALUE_MAX_LENGTH = 200

        // v1 tbl_host.contact_number 가 varchar(15) 라 PHONE 은 15자까지만 허용
        const val PHONE_MAX_LENGTH = 15
    }
}
