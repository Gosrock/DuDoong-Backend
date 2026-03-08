package band.gosrock.infrastructure.config.feign

import band.gosrock.infrastructure.outer.api.BaseFeignClientPackage
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import feign.Logger
import feign.codec.Decoder
import feign.jackson.JacksonDecoder
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

    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.FULL
}
