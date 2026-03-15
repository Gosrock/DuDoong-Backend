package band.gosrock.api.email.service

import band.gosrock.api.email.dto.OrderMailDto
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

@Service
class OrderApproveRequestEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(orderMailDto: OrderMailDto) {
        val context = Context()
        val userInfo = orderMailDto.userInfo
        context.setVariable("userInfo", userInfo)
        context.setVariable("orderInfo", orderMailDto.orderInfo)
        context.setVariable("eventInfo", orderMailDto.eventInfo)
        awsSesUtils.singleEmailRequest(
            userInfo,
            "두둥 주문승인 요청 알림드립니다.",
            "orderApproveRequest",
            context,
        )
    }
}
