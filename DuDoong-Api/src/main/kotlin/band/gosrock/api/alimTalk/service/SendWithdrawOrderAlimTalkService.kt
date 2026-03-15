package band.gosrock.api.alimTalk.service

import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto
import band.gosrock.domain.common.alarm.OrderKakaoTalkAlarm
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper
import org.springframework.stereotype.Service

@Service
class SendWithdrawOrderAlimTalkService(
    private val ncpHelper: NcpHelper,
) {
    fun execute(orderAlimTalkDto: OrderAlimTalkDto) {
        val userInfo = orderAlimTalkDto.userInfo
        val eventInfo = orderAlimTalkDto.eventInfo
        val orderInfo = orderAlimTalkDto.orderInfo

        val content = OrderKakaoTalkAlarm.deletionOf(
            userInfo.userName,
            eventInfo.hostName,
            eventInfo.eventName,
        )
        val headerContent = OrderKakaoTalkAlarm.deletionHeaderOf()

        ncpHelper.sendCancelOrderAlimTalk(
            userInfo.phoneNum,
            OrderKakaoTalkAlarm.deletionTemplateCode(),
            content,
            headerContent,
            orderInfo,
        )
    }
}
