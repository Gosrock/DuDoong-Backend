package band.gosrock.domain.domains.order.repository

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface OrderRepository : CrudRepository<Order, Long>, OrderCustomRepository {
    fun countByEventId(eventId: Long): Long

    fun findByEventId(eventId: Long): List<Order>
    fun findByEventIdAndOrderStatus(eventId: Long, orderStatus: OrderStatus): List<Order>
    fun findByEventIdAndUserIdAndOrderStatus(eventId: Long, userId: Long, orderStatus: OrderStatus): List<Order>
    fun findByUuidIn(uuids: List<String>): List<Order>

    /** Admin: 키워드(orderName/유저명/이벤트명) + 상태 + 이벤트 필터로 주문 검색 */
    @Query(
        """SELECT o FROM tbl_order o WHERE
            (:keyword IS NULL OR o.orderName LIKE %:keyword%
                OR EXISTS (SELECT 1 FROM User u WHERE u.id = o.userId AND u.profile.name LIKE %:keyword%)
                OR EXISTS (SELECT 1 FROM tbl_event e WHERE e.id = o.eventId AND e.eventBasic.name LIKE %:keyword%))
            AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
            AND (:eventId IS NULL OR o.eventId = :eventId)"""
    )
    fun findAllForAdmin(
        @Param("keyword") keyword: String?,
        @Param("orderStatus") orderStatus: OrderStatus?,
        @Param("eventId") eventId: Long?,
        pageable: Pageable
    ): Page<Order>

    /** Admin: 오늘 이후 생성된 주문 수 */
    fun countByCreatedAtAfter(after: LocalDateTime): Long

    /** Admin: 오늘 이후 생성된 특정 상태 주문 수 */
    fun countByCreatedAtAfterAndOrderStatus(after: LocalDateTime, orderStatus: OrderStatus): Long

    /** Admin: 오늘 이후 생성된 특정 상태 주문 목록 (매출 계산용) */
    fun findByCreatedAtAfterAndOrderStatusIn(after: LocalDateTime, orderStatuses: List<OrderStatus>): List<Order>

    /** Admin: 기간 내 주문 수 */
    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long

    /** Admin: 기간 내 특정 상태 주문 수 */
    fun countByCreatedAtBetweenAndOrderStatus(start: LocalDateTime, end: LocalDateTime, orderStatus: OrderStatus): Long

    /** Admin: 기간 내 특정 상태 주문 목록 (매출 계산용) */
    fun findByCreatedAtBetweenAndOrderStatusIn(start: LocalDateTime, end: LocalDateTime, orderStatuses: List<OrderStatus>): List<Order>

    /** Admin: 최근 주문 N건 */
    @Query("SELECT o FROM tbl_order o ORDER BY o.createdAt DESC")
    fun findTopNByOrderByCreatedAtDesc(pageable: Pageable): List<Order>

    /** Admin: 페이지네이션 없이 전체 주문 조회 (엑셀 다운로드용) */
    @Query(
        """SELECT o FROM tbl_order o WHERE
            (:keyword IS NULL OR o.orderName LIKE %:keyword%
                OR EXISTS (SELECT 1 FROM User u WHERE u.id = o.userId AND u.profile.name LIKE %:keyword%)
                OR EXISTS (SELECT 1 FROM tbl_event e WHERE e.id = o.eventId AND e.eventBasic.name LIKE %:keyword%))
            AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
            AND (:eventId IS NULL OR o.eventId = :eventId)"""
    )
    fun findAllForAdminNoPage(
        @Param("keyword") keyword: String?,
        @Param("orderStatus") orderStatus: OrderStatus?,
        @Param("eventId") eventId: Long?,
    ): List<Order>
}
