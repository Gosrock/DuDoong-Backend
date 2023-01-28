package band.gosrock.domain.domains.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType;
import org.junit.jupiter.api.BeforeEach;
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
}
