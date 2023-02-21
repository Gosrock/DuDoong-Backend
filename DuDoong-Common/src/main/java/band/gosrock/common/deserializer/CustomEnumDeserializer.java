package band.gosrock.common.deserializer;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Arrays;

public class CustomEnumDeserializer extends StdDeserializer<Enum<?>>
        implements ContextualDeserializer {

    public CustomEnumDeserializer() {
        this(null);
    }

    protected CustomEnumDeserializer(Class<?> vc) {
        super(vc);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = jp.getCodec().readTree(jp);
        JsonNode nameNode = jsonNode.get("name");
        if (nameNode == null) return null;
        String text = jsonNode.asText();
        Class<? extends Enum> enumType = (Class<? extends Enum>) this._valueClass;
        return Arrays.stream(enumType.getEnumConstants())
                .filter(constant -> constant.name().equals(text))
                .findAny()
                .orElse(null);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JsonMappingException {
        return new CustomEnumDeserializer(property.getType().getRawClass());
    }
}
