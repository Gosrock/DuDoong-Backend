package band.gosrock.infrastructure.config.alilmTalk

import band.gosrock.common.annotation.Helper
import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkOrderInfo
import band.gosrock.infrastructure.config.alilmTalk.dto.MessageDto
import band.gosrock.infrastructure.outer.api.alimTalk.client.NcpClient
import org.apache.commons.codec.binary.Base64
import org.springframework.beans.factory.annotation.Value
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Helper
class NcpHelper(
    private val ncpClient: NcpClient,
    @Value("\${ncp.service-id}") private val serviceID: String,
    @Value("\${ncp.access-key}") private val ncpAccessKey: String,
    @Value("\${ncp.secret-key}") private val ncpSecretKey: String,
    @Value("\${ncp.plus-friend-id}") private val plusFriendId: String,
    private val springEnvironmentHelper: SpringEnvironmentHelper,
) {
    companion object {
        const val space = " "
        const val newLine = "\n"
        const val method = "POST"
    }

    // 주문 취소 알림톡 (아이템리스트+버튼)
    fun sendCancelOrderAlimTalk(
        to: String,
        templateCode: String,
        content: String,
        headerContent: String,
        orderInfo: AlimTalkOrderInfo,
    ) {
        if (!springEnvironmentHelper.isProdAndStagingProfile()) return
        val timeStamp = Instant.now().toEpochMilli().toString()
        val url = "/alimtalk/v2/services/$serviceID/messages"
        val signature = makePostSignature(ncpAccessKey, ncpSecretKey, url, timeStamp)
        val body = makeCancelOrderBody(templateCode, to, content, headerContent, orderInfo)
        ncpClient.sendItemButtonAlimTalk(serviceID, ncpAccessKey, timeStamp, signature, body)
    }

    // 회원 가입 알림톡 (버튼)
    fun sendButtonNcpAlimTalk(to: String, templateCode: String, content: String) {
        if (!springEnvironmentHelper.isProdAndStagingProfile()) return
        val timeStamp = Instant.now().toEpochMilli().toString()
        val url = "/alimtalk/v2/services/$serviceID/messages"
        val signature = makePostSignature(ncpAccessKey, ncpSecretKey, url, timeStamp)
        val body = makeButtonBody(templateCode, to, content)
        ncpClient.sendButtonAlimTalk(serviceID, ncpAccessKey, timeStamp, signature, body)
    }

    // 주문 성공 알림톡 (아이템리스트+버튼)
    fun sendDoneOrderAlimTalk(
        to: String,
        templateCode: String,
        content: String,
        headerContent: String,
        orderInfo: AlimTalkOrderInfo,
    ) {
        if (!springEnvironmentHelper.isProdAndStagingProfile()) return
        val timeStamp = Instant.now().toEpochMilli().toString()
        val url = "/alimtalk/v2/services/$serviceID/messages"
        val signature = makePostSignature(ncpAccessKey, ncpSecretKey, url, timeStamp)
        val body = makeDoneOrderBody(templateCode, to, content, headerContent, orderInfo)
        ncpClient.sendItemButtonAlimTalk(serviceID, ncpAccessKey, timeStamp, signature, body)
    }

    // 주문서 전송 알림톡 (아이템리스트)
    fun sendSettlementNcpAlimTalk(
        to: String,
        templateCode: String,
        content: String,
        headerContent: String,
        email: String,
        eventName: String,
    ) {
        if (!springEnvironmentHelper.isProdAndStagingProfile()) return
        val timeStamp = Instant.now().toEpochMilli().toString()
        val url = "/alimtalk/v2/services/$serviceID/messages"
        val signature = makePostSignature(ncpAccessKey, ncpSecretKey, url, timeStamp)
        val body = makeSettlementItemBody(templateCode, to, content, headerContent, email, eventName)
        ncpClient.sendItemAlimTalk(serviceID, ncpAccessKey, timeStamp, signature, body)
    }

    fun makeDoneOrderBody(
        templateCode: String,
        to: String,
        content: String,
        headerContent: String,
        orderInfo: AlimTalkOrderInfo,
    ): MessageDto.AlimTalkItemButtonBody {
        val item = makeOrderItem(orderInfo)
        val buttons = makeDoneOrderButtons()
        val message = MessageDto.AlimTalkItemButtonMessage(
            to = to, content = content, headerContent = headerContent,
            item = item, buttons = buttons,
        )
        return MessageDto.AlimTalkItemButtonBody(
            plusFriendId = plusFriendId,
            templateCode = templateCode,
            messages = listOf(message),
        )
    }

    fun makeCancelOrderBody(
        templateCode: String,
        to: String,
        content: String,
        headerContent: String,
        orderInfo: AlimTalkOrderInfo,
    ): MessageDto.AlimTalkItemButtonBody {
        val item = makeOrderItem(orderInfo)
        val buttons = makeCancelOrderButtons()
        val message = MessageDto.AlimTalkItemButtonMessage(
            to = to, content = content, headerContent = headerContent,
            item = item, buttons = buttons,
        )
        return MessageDto.AlimTalkItemButtonBody(
            plusFriendId = plusFriendId,
            templateCode = templateCode,
            messages = listOf(message),
        )
    }

    fun makeSettlementItemBody(
        templateCode: String,
        to: String,
        content: String,
        headerContent: String,
        email: String,
        eventName: String,
    ): MessageDto.AlimTalkItemBody {
        val item = makeSettlementItem(email, eventName)
        val message = MessageDto.AlimTalkItemMessage(
            to = to, content = content, headerContent = headerContent, item = item,
        )
        return MessageDto.AlimTalkItemBody(
            plusFriendId = plusFriendId,
            templateCode = templateCode,
            messages = listOf(message),
        )
    }

    fun makeItemBody(
        templateCode: String,
        to: String,
        content: String,
        headerContent: String,
        orderInfo: AlimTalkOrderInfo,
    ): MessageDto.AlimTalkItemBody {
        val item = makeOrderItem(orderInfo)
        val message = MessageDto.AlimTalkItemMessage(
            to = to, content = content, headerContent = headerContent, item = item,
        )
        return MessageDto.AlimTalkItemBody(
            plusFriendId = plusFriendId,
            templateCode = templateCode,
            messages = listOf(message),
        )
    }

    fun makeButtonBody(templateCode: String, to: String, content: String): MessageDto.AlimTalkButtonBody {
        val buttons = makeSignUpButtons()
        val message = MessageDto.AlimTalkButtonMessage(to = to, content = content, buttons = buttons)
        return MessageDto.AlimTalkButtonBody(
            plusFriendId = plusFriendId,
            templateCode = templateCode,
            messages = listOf(message),
        )
    }

    fun makeSettlementItem(email: String, eventName: String): MessageDto.AlimTalkItem =
        MessageDto.AlimTalkItem(
            list = listOf(
                MessageDto.Item(title = "이메일 :", description = email),
                MessageDto.Item(title = "이벤트 :", description = eventName),
            ),
        )

    fun makeOrderItem(orderInfo: AlimTalkOrderInfo): MessageDto.AlimTalkItem =
        MessageDto.AlimTalkItem(
            list = listOf(
                MessageDto.Item(title = "주문명 :", description = orderInfo.name),
                MessageDto.Item(title = "수량 :", description = orderInfo.quantity.toString()),
                MessageDto.Item(title = "가격 :", description = orderInfo.money),
                MessageDto.Item(
                    title = "주문일시 :",
                    description = orderInfo.createAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                ),
            ),
        )

    fun makeHomePageButton(): MessageDto.AlimTalkButton =
        MessageDto.AlimTalkButton(
            type = "WL", name = "홈페이지 바로가기",
            linkMobile = "https://dudoong.com/", linkPc = "https://dudoong.com/",
        )

    fun makeAddChannelButton(): MessageDto.AlimTalkButton =
        MessageDto.AlimTalkButton(type = "AC", name = "채널 추가")

    fun makeMyPageButton(): MessageDto.AlimTalkButton =
        MessageDto.AlimTalkButton(
            type = "WL", name = "마이페이지 바로가기",
            linkMobile = "https://dudoong.com/mypage", linkPc = "https://dudoong.com/mypage",
        )

    fun makeSignUpButtons(): List<MessageDto.AlimTalkButton> =
        listOf(makeAddChannelButton(), makeHomePageButton())

    fun makeDoneOrderButtons(): List<MessageDto.AlimTalkButton> =
        listOf(makeMyPageButton())

    fun makeCancelOrderButtons(): List<MessageDto.AlimTalkButton> =
        listOf(makeHomePageButton())

    fun makePostSignature(accessKey: String, secretKey: String, url: String, timeStamp: String): String {
        return try {
            val message = "$method$space$url$newLine$timeStamp$newLine$accessKey"
            val signingKey = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(signingKey)
            val rawHmac = mac.doFinal(message.toByteArray(Charsets.UTF_8))
            Base64.encodeBase64String(rawHmac)
        } catch (ex: Exception) {
            throw DuDoongDynamicException(0, "400", ex.message ?: "")
        }
    }
}
