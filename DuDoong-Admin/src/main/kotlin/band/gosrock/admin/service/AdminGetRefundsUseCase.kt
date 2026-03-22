package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminRefundResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetRefundsUseCase(
    private val orderAdaptor: OrderAdaptor,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(
        userId: Long,
        refundStatus: RefundStatus?,
        eventId: Long?,
        keyword: String?,
        pageable: Pageable,
    ): Page<AdminRefundResponse> {
        adminAuthValidator.validateAdminOrAbove(userId)
        val orderPage = orderAdaptor.findRefunds(eventId, refundStatus, keyword, pageable)

        val userIds = orderPage.content.mapNotNull { it.userId }
        val eventIds = orderPage.content.mapNotNull { it.eventId }

        val userMap = userRepository.findAllByIdIn(userIds).associateBy { it.id }
        val eventMap = eventRepository.findAllByIdIn(eventIds).associateBy { it.id }

        return orderPage.map { order ->
            val userName = order.userId?.let { userMap[it]?.profile?.name }
            val eventName = order.eventId?.let { eventMap[it]?.eventBasic?.name }
            AdminRefundResponse.of(order, userName, eventName)
        }
    }
}
