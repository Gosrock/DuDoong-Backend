package band.gosrock.domain.common.model

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @Column(updatable = false)
    @CreatedDate
    var createdAt: LocalDateTime? = null
        protected set

    @Column
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
        protected set

    /** Kotlin 호환용 메서드 - 같은 모듈에서 Kotlin이 Lombok getter에 접근 불가한 문제 우회 */
    fun createdAtKt(): LocalDateTime = this.createdAt!!
}
