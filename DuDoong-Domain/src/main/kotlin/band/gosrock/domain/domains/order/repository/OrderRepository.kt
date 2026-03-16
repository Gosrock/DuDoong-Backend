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
    fun findByEventId(eventId: Long): List<Order>
    fun findByEventIdAndOrderStatus(eventId: Long, orderStatus: OrderStatus): List<Order>
    fun findByEventIdAndUserIdAndOrderStatus(eventId: Long, userId: Long, orderStatus: OrderStatus): List<Order>
    fun findByUuidIn(uuids: List<String>): List<Order>

    /** Admin: 키워드(orderName) + 상태 필터로 주문 검색 */
    @Query(
        "SELECT o FROM tbl_order o WHERE " +
            "(:keyword IS NULL OR o.orderName LIKE %:keyword%) " +
            "AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)"
    )
    fun findAllForAdmin(
        @Param("keyword") keyword: String?,
        @Param("orderStatus") orderStatus: OrderStatus?,
        pageable: Pageable
    ): Page<Order>

    /** Admin: 오늘 이후 생성된 주문 수 */
    fun countByCreatedAtAfter(after: LocalDateTime): Long

    /** Admin: 오늘 이후 생성된 특정 상태 주문 수 */
    fun countByCreatedAtAfterAndOrderStatus(after: LocalDateTime, orderStatus: OrderStatus): Long

    /** Admin: 오늘 이후 생성된 특정 상태 주문 목록 (매출 계산용) */
    fun findByCreatedAtAfterAndOrderStatusIn(after: LocalDateTime, orderStatuses: List<OrderStatus>): List<Order>
}
