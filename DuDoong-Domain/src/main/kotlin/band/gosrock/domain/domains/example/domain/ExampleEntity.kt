package band.gosrock.domain.domains.example.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

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
