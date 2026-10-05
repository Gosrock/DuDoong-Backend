package band.gosrock.domain.domains.notification.repository

import band.gosrock.domain.domains.notification.domain.Notification
import band.gosrock.domain.domains.notification.domain.NotificationType
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findSliceByUserIdOrderByIdDesc(userId: Long, pageable: Pageable): Slice<Notification>

    fun countByUserIdAndIsReadFalse(userId: Long): Long

    fun findAllByUserId(userId: Long): List<Notification>

    @Modifying(clearAutomatically = true)
    @Query(
        "update tbl_notification n set n.isRead = true, n.readAt = :now, n.updatedAt = :now " +
            "where n.userId = :userId and n.isRead = false and n.id in :ids",
    )
    fun markReadByIds(@Param("userId") userId: Long, @Param("ids") ids: Collection<Long>, @Param("now") now: LocalDateTime): Int

    @Modifying(clearAutomatically = true)
    @Query("update tbl_notification n set n.isRead = true, n.readAt = :now, n.updatedAt = :now where n.userId = :userId and n.isRead = false")
    fun markAllRead(@Param("userId") userId: Long, @Param("now") now: LocalDateTime): Int

    /** 안 읽은 알림 중 한 종류 (최신 순). 티켓탭 공지 바 T-3 = 안 읽은 ORDER_APPROVED (#719) */
    fun findAllByUserIdAndTypeAndIsReadFalseOrderByIdDesc(userId: Long, type: NotificationType): List<Notification>

    fun existsByUserIdAndTypeAndTargetIdAndIsReadFalse(userId: Long, type: NotificationType, targetId: String): Boolean

    /** 한 종류·한 대상의 안 읽은 알림 읽음 처리 (T-2 로 승인된 주문의 티켓을 열면 그 주문의 승인 알림을 읽음, #719) */
    @Modifying(clearAutomatically = true)
    @Query(
        "update tbl_notification n set n.isRead = true, n.readAt = :now, n.updatedAt = :now " +
            "where n.userId = :userId and n.isRead = false and n.type = :type and n.targetId = :targetId",
    )
    fun markReadByTarget(
        @Param("userId") userId: Long,
        @Param("type") type: NotificationType,
        @Param("targetId") targetId: String,
        @Param("now") now: LocalDateTime,
    ): Int
}
