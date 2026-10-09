package band.gosrock.api.config

import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 운영(prod)에서 비밀값이 저장소에 공개된 yml 기본값 그대로인지 기동 때 알린다 (#764).
 * 기동은 막지 않는다(운영 env 변경 없이 배포하기 위해). ERROR 로그와 Slack 알림에는 설정 이름만 남기고 값은 남기지 않는다.
 * 기본값은 yml 의 `${ENV:기본값}` 에서 읽으므로 yml 기본값이 바뀌어도 같이 따라간다
 */
@Component
class DefaultSecretsWarner(
    private val environment: Environment,
    private val slackErrorNotificationProvider: SlackErrorNotificationProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun warnIfDefaultSecrets() {
        if (!environment.activeProfiles.contains("prod")) return
        val names = defaultSecretsInUse()
        if (names.isEmpty()) return
        log.error("운영에서 공개된 기본 비밀값 사용 중: {}", names.joinToString(", "))
        try {
            slackErrorNotificationProvider.sendNotification(
                listOf(section { it.text(MarkdownTextObject.builder().text("*운영에서 공개된 기본 비밀값 사용 중*\n${names.joinToString(", ")}").build()) }),
            )
        } catch (e: Exception) {
            log.warn("기본 비밀값 Slack 알림 실패: {}", e.toString())
        }
    }

    /** 현재 값이 yml 공개 기본값과 같은 설정 이름 */
    fun defaultSecretsInUse(): List<String> {
        val defaults = SECRET_SOURCES.flatMap { (file, keys) ->
            val yaml = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource(file)) }.`object` ?: return@flatMap emptyList()
            keys.mapNotNull { key -> yaml.getProperty(key)?.let { DEFAULT_IN_PLACEHOLDER.find(it)?.groupValues?.get(1) }?.let { key to it } }
        }
        return defaults.filter { (key, default) -> environment.getProperty(key) == default }.map { it.first }
    }

    companion object {
        private val SECRET_SOURCES = listOf(
            "application-common.yml" to listOf("auth.jwt.secret-key", "toss.secret-key", "toss.mid"),
            "application-infrastructure.yml" to listOf("aws.access-key", "aws.secret-key"),
        )

        /** `${ENV_NAME:기본값}` 의 기본값 */
        private val DEFAULT_IN_PLACEHOLDER = Regex("^\\$\\{[A-Za-z0-9_]+:(.+)}$")
    }
}
