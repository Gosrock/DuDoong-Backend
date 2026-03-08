package band.gosrock.infrastructure.config.slack

import com.slack.api.Slack
import com.slack.api.webhook.Payload
import org.apache.commons.codec.binary.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.UnknownHostException

@Service
class SlackMessageProvider(
    @Value("\${slack.webhook.username}") private val username: String,
    @Value("\${slack.webhook.icon-url}") private val iconUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 이벤트 핸들러 자체에서 비동기로 실행하기 때문에 @Async 어노테이션 지움 */
    fun sendMessage(url: String?, text: String) {
        if (url == null) return
        try {
            doSend(url, text)
        } catch (e: Exception) {
            // ignored
        }
    }

    /** 호스트가 존재하는 지 확인하기 위해 동기로 처리 */
    @Throws(UnknownHostException::class)
    fun register(url: String) {
        val text = "두둥 슬랙 알림이 성공적으로 등록되었습니다!"
        doSend(url, text)
    }

    @Throws(UnknownHostException::class)
    private fun doSend(url: String, text: String) {
        val slack = Slack.getInstance()
        val payload = Payload.builder().text(text).username(username).iconUrl(iconUrl).build()
        try {
            val responseBody = slack.send(url, payload).body
            if (!StringUtils.equals(responseBody, "ok")) {
                throw UnknownHostException("올바른 슬랙 URL이 아닙니다.")
            }
        } catch (e: UnknownHostException) {
            throw e
        } catch (e: IOException) {
            log.error(e.message, e)
            throw RuntimeException(e)
        }
    }
}
