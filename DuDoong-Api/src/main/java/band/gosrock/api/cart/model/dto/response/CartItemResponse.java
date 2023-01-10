package band.gosrock.api.cart.model.dto.response;


import band.gosrock.domain.common.vo.OptionAnswerVo;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    // 일반티켓(1/3) - 4000원
    private String name;
    // 응답 목록
    private List<OptionAnswerVo> answers;

    public static CartItemResponse of(String name, List<OptionAnswerVo> optionAnswerVos) {
        return CartItemResponse.builder().answers(optionAnswerVos).name(name).build();
    }
}
