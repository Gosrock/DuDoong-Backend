package band.gosrock.domain.domains.ticket_item.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class OptionGroupType(
    val value: String,
    @JsonValue val displayName: String,
) {
    // T/F
    TRUE_FALSE("TRUE_FALSE", "Y/N"),
    //
    MULTIPLE_CHOICE("MULTIPLE_CHOICE", "객관식"),
    //
    SUBJECTIVE("SUBJECTIVE", "주관식"),
}
