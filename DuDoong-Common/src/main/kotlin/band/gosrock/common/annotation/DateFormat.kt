package band.gosrock.common.annotation

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonFormat

@JacksonAnnotationsInside
@Retention(AnnotationRetention.RUNTIME)
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm", timezone = "Asia/Seoul")
annotation class DateFormat
