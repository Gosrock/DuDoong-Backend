package band.gosrock.domain.domains.notification.repository

import band.gosrock.domain.domains.notification.domain.Notification
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
}
