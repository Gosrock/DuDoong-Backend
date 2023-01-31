package band.gosrock.domain.domains.cart.domain;

import static org.junit.jupiter.api.Assertions.*;
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
class CartOptionAnswerTest {
    @Mock Option option;

    CartOptionAnswer cartOptionAnswer;

    private final Money W3000 = Money.wons(3000L);
    private final long optionId = 1L;
    private final String answer = "답변";

    @BeforeEach
    void setUp() {
        given(option.getId()).willReturn(optionId);
        given(option.getAdditionalPrice()).willReturn(W3000);

        cartOptionAnswer = CartOptionAnswer.builder().answer(answer).option(option).build();
    }

    @Test
    void 주문옵션답변_옵션답변정보_변환_검증() {
        // given
        String questionName = "질문이름";
        String questionDescription = "질문설명";
        OptionGroupType trueFalse = OptionGroupType.TRUE_FALSE;
        given(option.getQuestionDescription()).willReturn(questionDescription);
        given(option.getQuestionType()).willReturn(trueFalse);
        given(option.getQuestionName()).willReturn(questionName);
        OptionAnswerVo build =
                OptionAnswerVo.builder()
                        .answer(answer)
                        .optionGroupType(trueFalse)
                        .questionDescription(questionDescription)
                        .questionName(questionName)
                        .additionalPrice(W3000)
                        .build();
        // when
        OptionAnswerVo optionAnswerVo = cartOptionAnswer.getOptionAnswerVo(option);
        assertEquals(optionAnswerVo, build);
    }
}
