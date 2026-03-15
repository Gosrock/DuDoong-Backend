package band.gosrock.domain.domains.example.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "tbl_example")
@Entity
class ExampleEntity protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    var content: String? = null
        protected set

    constructor(content: String) : this() {
        this.content = content
    }
}
