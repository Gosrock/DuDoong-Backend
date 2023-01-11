package band.gosrock.domain.domains.ticket_item.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OptionGroupType {
    // T/F
    TRUE_FALSE("TRUE_FALSE","Y/N"),
    //
    MULTIPLE_CHOICE("MULTIPLE_CHOICE","객관식"),
    //
    SUBJECTIVE("SUBJECTIVE","주관식");
    private String value;

    @JsonValue
    private String name;
}
