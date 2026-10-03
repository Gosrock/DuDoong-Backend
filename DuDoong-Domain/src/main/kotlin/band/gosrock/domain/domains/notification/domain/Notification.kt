package band.gosrock.domain.domains.notification.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * v2 알림센터 알림 (#714, V005). 저장은 도메인 이벤트 핸들러(커밋 후 비동기)가 [band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService] 로 한다.
 *
 * - uk(user_id, type, dedup_key): 같은 원인으로 같은 사람에게 두 번 저장하지 않는다 (이벤트 재처리·중복 발행). 이미 있으면 건너뛴다 ([band.gosrock.domain.domains.notification.repository.NotificationBulkRepository])
 *   dedup_key 는 주문 알림이면 orderUuid, 멤버 추가면 `host_user:{host_user_id}` (삭제 후 재추가는 새 행이라 새 알림)
 * - idx(user_id, is_read): 안읽음 수 / 전체 읽음
 * - idx(user_id): 목록 최신순 (InnoDB 보조 인덱스에 PK 가 붙어 user_id 조건 + notification_id 역순 정렬을 인덱스로 처리)
 */
@Entity(name = "tbl_notification")
@Table(
    uniqueConstraints = [UniqueConstraint(name = "uk_notification_user_type_dedup", columnNames = ["user_id", "type", "dedup_key"])],
    indexes = [
        Index(name = "idx_notification_user_id_is_read", columnList = "user_id, is_read"),
        Index(name = "idx_notification_user_id", columnList = "user_id"),
    ],
)
class Notification(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    val type: NotificationType,

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    val title: String,

    @Column(name = "body", nullable = false, length = BODY_MAX_LENGTH)
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    val targetType: NotificationTargetType,

    @Column(name = "target_id", nullable = false, length = 64)
    val targetId: String,

    /** ORDER 대상의 공연 id (딥링크 `/events/{eventId}/orders/{orderUuid}`). HOST 대상은 null */
    @Column(name = "event_id")
    val eventId: Long? = null,

    /** 부가 정보 JSON 문자열 (호스트명, 역할, 공연명, 거절 사유 등) */
    @Column(name = "extra", length = EXTRA_MAX_LENGTH)
    val extra: String? = null,

    @Column(name = "dedup_key", nullable = false, length = 100)
    val dedupKey: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    var id: Long? = null
        protected set

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
        protected set

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null
        protected set

    companion object {
        const val TITLE_MAX_LENGTH = 100
        const val BODY_MAX_LENGTH = 500
        const val EXTRA_MAX_LENGTH = 1000
    }
}
