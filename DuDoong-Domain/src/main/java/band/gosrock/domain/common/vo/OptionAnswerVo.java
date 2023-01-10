package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OptionAnswerVo {
    private OptionGroupType optionGroupType;
    private String questionName;
    private String questionDescription;
    private String answer;
    private String additionalPrice;
}
