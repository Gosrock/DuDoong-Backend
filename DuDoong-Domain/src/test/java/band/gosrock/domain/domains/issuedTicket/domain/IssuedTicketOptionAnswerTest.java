package band.gosrock.domain.domains.issuedTicket.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuedTicketOptionAnswerTest {

    @Mock Option option;

    IssuedTicketOptionAnswer issuedTicketOptionAnswer;

    private final Money W3000 = Money.wons(3000L);
    private final Long optionId = 1L;
    private final String answer = "답변";

    private final Long orderOptionAnswerId = 1L;

    @BeforeEach
    void setUp() {
        // option 세팅
        given(option.getAdditionalPrice()).willReturn(W3000);

        issuedTicketOptionAnswer =
                IssuedTicketOptionAnswer.builder().optionId(optionId).answer(answer).build();
    }

    @Test
    void 주문옵션답변_발급티켓옵션답변_변환_테스트() {
        // given
        String questionName = "questionName";
        String questionDescription = "questionDescription";
        OptionGroupType optionGroupType = OptionGroupType.TRUE_FALSE;

        given(option.getQuestionName()).willReturn(questionName);
        given(option.getQuestionDescription()).willReturn(questionDescription);
        given(option.getQuestionType()).willReturn(optionGroupType);

        OptionAnswerVo optionAnswerVoForTest =
                OptionAnswerVo.builder()
                        .answer(answer)
                        .additionalPrice(W3000)
                        .optionGroupType(optionGroupType)
                        .questionDescription(questionDescription)
                        .questionName(questionName)
                        .build();

        OptionAnswerVo optionAnswerVo = issuedTicketOptionAnswer.getOptionAnswerVo(option);

        assertEquals(optionAnswerVo, optionAnswerVoForTest);
    }
}
