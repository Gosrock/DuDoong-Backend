package band.gosrock.common.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class CustomEnumDeserializer(vc: Class<*>? = null) :
    StdDeserializer<Enum<*>>(vc),
    ContextualDeserializer {

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Enum<*>? {
        val jsonNode = jp.codec.readTree<com.fasterxml.jackson.databind.JsonNode>(jp)
        val text = jsonNode.asText()
        val enumType = _valueClass as? Class<out Enum<*>> ?: return null
        return enumType.enumConstants?.firstOrNull { it.name == text }
    }

    @Throws(JsonMappingException::class)
    override fun createContextual(
        ctxt: DeserializationContext,
        property: BeanProperty,
    ): JsonDeserializer<*> {
        return CustomEnumDeserializer(property.type.rawClass)
    }
}
