package band.gosrock.domain.domains.order.domain;

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
class OrderOptionAnswerTest {

    @Mock Option option;

    Money wons3000 = Money.wons(3000L);

    String answer = "응답";

    String questionDescription = "질문설명";
    String questionName = "질문이름";
    OrderOptionAnswer orderOptionAnswer;
    OptionGroupType optionGroupType = OptionGroupType.TRUE_FALSE;

    OptionAnswerVo optionAnswerVo;

    @BeforeEach
    void setUp() {
        orderOptionAnswer = OrderOptionAnswer.builder().answer(answer).option(option).build();
        optionAnswerVo =
                OptionAnswerVo.builder()
                        .questionDescription(questionDescription)
                        .answer(answer)
                        .optionGroupType(optionGroupType)
                        .questionName(questionName)
                        .additionalPrice(wons3000)
                        .build();
    }

    @Test
    void 옵션_가격조회() {
        // given
        given(option.getAdditionalPrice()).willReturn(wons3000);
        // when
        Money optionPrice = orderOptionAnswer.getOptionPrice();
        // then
        assertEquals(optionPrice, wons3000);
    }

    @Test
    void 옵션_질문_이름_조회() {
        // given
        given(option.getQuestionName()).willReturn(questionName);
        // when
        String expectedQuestionName = orderOptionAnswer.getQuestionName();
        // then
        assertEquals(expectedQuestionName, questionName);
    }

    @Test
    void 옵션_질문_설명_조회() {
        // given
        given(option.getQuestionDescription()).willReturn(questionDescription);

        // when
        String expectedQuestionName = orderOptionAnswer.getQuestionDescription();
        // then
        assertEquals(expectedQuestionName, questionDescription);
    }

    @Test
    void 옵션_질문_타입_조회() {
        // given
        given(option.getQuestionType()).willReturn(optionGroupType);
        // when
        OptionGroupType expectedQuestionType = orderOptionAnswer.getQuestionType();
        // then
        assertEquals(expectedQuestionType, optionGroupType);
    }

    @Test
    void 옵션응답_VO_생성_검증() {
        // given
        given(option.getQuestionType()).willReturn(optionGroupType);
        given(option.getQuestionDescription()).willReturn(questionDescription);
        given(option.getQuestionName()).willReturn(questionName);
        given(option.getAdditionalPrice()).willReturn(wons3000);

        // when
        OptionAnswerVo expectedOptionAnswerVo = orderOptionAnswer.getOptionAnswerVo();
        // then
        assertEquals(expectedOptionAnswerVo, optionAnswerVo);
    }
}
