package band.gosrock.common.annotation

import band.gosrock.common.deserializer.CustomEnumDeserializer
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = CustomEnumDeserializer::class)
annotation class EnumClass
