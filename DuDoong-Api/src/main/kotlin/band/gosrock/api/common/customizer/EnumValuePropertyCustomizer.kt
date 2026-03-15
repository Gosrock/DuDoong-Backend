package band.gosrock.api.common.customizer

import com.fasterxml.jackson.databind.JavaType
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.PropertyCustomizer
import org.springframework.stereotype.Component

// https://stackoverflow.com/questions/68747036/how-can-have-springdoc-openapi-use-the-jsonvalue-enum-format-without-changing-t

/** enum jsonvalue 어노테이션 사용할때 예시값을 보여주기 위함. */
@Component
class EnumValuePropertyCustomizer : PropertyCustomizer {
    override fun customize(property: Schema<*>, type: AnnotatedType): Schema<*> {
        if (property is StringSchema && isEnumType(type)) {
            val objectMapper = Json.mapper()
            property.setEnum(
                (type.type as JavaType).rawClass.enumConstants
                    ?.map { e -> objectMapper.convertValue(e, String::class.java) }
                    ?: emptyList()
            )
        }
        return property
    }

    private fun isEnumType(type: AnnotatedType): Boolean {
        return type.type is JavaType && (type.type as JavaType).isEnumType
    }
}
