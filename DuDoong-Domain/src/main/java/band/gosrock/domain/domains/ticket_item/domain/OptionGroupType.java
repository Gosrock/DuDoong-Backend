package band.gosrock.domain.domains.ticket_item.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OptionGroupType {
    // T/F
    TRUE_FALSE("TRUE_FALSE"),
    //
    MULTIPLE_CHOICE("MULTIPLE_CHOICE"),
    //
    SUBJECTIVE("SUBJECTIVE");
    private String value;
}
