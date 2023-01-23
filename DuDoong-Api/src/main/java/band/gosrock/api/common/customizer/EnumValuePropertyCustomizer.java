package band.gosrock.api.common.customizer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.stereotype.Component;

@Component
public class EnumValuePropertyCustomizer implements PropertyCustomizer {
    @Override
    public Schema customize(Schema property, AnnotatedType type) {
        if (property instanceof StringSchema && isEnumType(type)) {
            ObjectMapper objectMapper = Json.mapper();

            property.setEnum(Arrays.stream(((JavaType) type.getType()).getRawClass().getEnumConstants())
                .map(e -> objectMapper.convertValue(e, String.class))
                .collect(Collectors.toList()));
        }
        return property;
    }

    private boolean isEnumType(AnnotatedType type) {
        return type.getType() instanceof JavaType t && t.isEnumType();
    }
}