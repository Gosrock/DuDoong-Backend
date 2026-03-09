package band.gosrock.domain.common.model;


import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column @LastModifiedDate private LocalDateTime updatedAt;

    /** Kotlin 호환용 메서드 - 같은 모듈에서 Kotlin이 Lombok getter에 접근 불가한 문제 우회 */
    public LocalDateTime createdAtKt() {
        return this.createdAt;
    }
}
