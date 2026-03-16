package band.gosrock.domain.common.converter

import java.math.BigDecimal
import java.math.RoundingMode
import jakarta.persistence.AttributeConverter

class BigDecimalScale6WithBankersRoundingConverter : AttributeConverter<BigDecimal, String> {

    override fun convertToDatabaseColumn(attribute: BigDecimal?): String {
        if (attribute == null) return BigDecimal.ZERO.toString()
        return attribute.setScale(6, RoundingMode.HALF_EVEN).toString()
    }

    override fun convertToEntityAttribute(dbData: String?): BigDecimal {
        if (dbData == null) return BigDecimal.ZERO
        return BigDecimal(dbData)
    }
}
