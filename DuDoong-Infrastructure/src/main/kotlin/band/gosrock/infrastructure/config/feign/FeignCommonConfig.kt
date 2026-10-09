package band.gosrock.infrastructure.config.feign

import band.gosrock.infrastructure.outer.api.BaseFeignClientPackage
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import feign.Logger
import feign.codec.Decoder
import feign.jackson.JacksonDecoder
import feign.slf4j.Slf4jLogger
import org.springframework.cloud.openfeign.FeignLoggerFactory
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableFeignClients(basePackageClasses = [BaseFeignClientPackage::class])
class FeignCommonConfig {

    @Bean
    fun feignDecoder(): Decoder = JacksonDecoder(customObjectMapper())

    fun customObjectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
        }

    /** 요청 줄·응답 상태·시간만 남긴다 (#764). 헤더·본문(토스 인증·결제 정보)은 남기지 않는다 */
    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.BASIC

    /** 레벨이 HEADERS 이상으로 바뀌어도 Authorization 헤더는 남기지 않는다 (#764). 로거 이름은 기본과 같은 클라이언트 인터페이스 이름 */
    @Bean
    fun feignLoggerFactory(): FeignLoggerFactory = FeignLoggerFactory { type -> AuthorizationHidingLogger(type) }

    class AuthorizationHidingLogger(type: Class<*>) : Slf4jLogger(type) {
        public override fun shouldLogRequestHeader(header: String): Boolean = !header.equals("Authorization", ignoreCase = true)
    }
}
