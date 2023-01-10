package band.gosrock.domain.common.converter;


import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.AttributeConverter;

public class BigDecimalScale6WithBankersRoundingConverter
        implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) {
            return BigDecimal.ZERO.toString();
        }
        return attribute.setScale(6, RoundingMode.HALF_EVEN).toString();
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(dbData);
    }
}
