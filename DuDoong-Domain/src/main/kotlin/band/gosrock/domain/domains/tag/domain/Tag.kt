package band.gosrock.domain.domains.tag.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 공연 태그 (v2). 운영자가 관리하며 (DEC-014) 초기 목록은 V002 SQL 로 넣는다 */
@Entity(name = "tbl_tag")
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_tag_category_name", columnNames = ["category", "name"])])
class Tag(
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    var category: TagCategory,

    @Column(length = 30, nullable = false)
    var name: String,

    @Column(nullable = false)
    var sortOrder: Int = 0,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    var id: Long? = null
        protected set
}
