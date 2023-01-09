package band.gosrock.api.cart.model.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemOptionAnswerResponse {
    // 질문
    private String question;
    // 네 (+1000원)
    private String answer;
}
