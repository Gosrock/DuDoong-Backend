package band.gosrock.common.annotation

import org.springframework.stereotype.Component

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class ExplainError(
    val value: String = "",
)
