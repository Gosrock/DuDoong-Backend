package band.gosrock.api.alimTalk.service

import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto
import band.gosrock.domain.common.alarm.OrderKakaoTalkAlarm
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper
import org.springframework.stereotype.Service

@Service
class SendDoneOrderAlimTalkService(
    private val ncpHelper: NcpHelper,
) {
    fun execute(orderAlimTalkDto: OrderAlimTalkDto) {
        val userInfo = orderAlimTalkDto.userInfo
        val eventInfo = orderAlimTalkDto.eventInfo
        val orderInfo = orderAlimTalkDto.orderInfo

        val content = OrderKakaoTalkAlarm.creationOf(
            userInfo.userName,
            eventInfo.hostName,
            eventInfo.eventName,
        )
        val headerContent = OrderKakaoTalkAlarm.creationHeaderOf()

        ncpHelper.sendDoneOrderAlimTalk(
            userInfo.phoneNum,
            OrderKakaoTalkAlarm.creationTemplateCode(),
            content,
            headerContent,
            orderInfo,
        )
    }
}
