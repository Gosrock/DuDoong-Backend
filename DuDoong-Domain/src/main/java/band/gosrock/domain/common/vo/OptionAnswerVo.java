package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class OptionAnswerVo {
    private OptionGroupType optionGroupType;
    private String questionName;
    private String questionDescription;
    private String answer;
    private Money additionalPrice;
}
