package band.gosrock.api.cart.model.dto.response;


import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    // 일반티켓(1/3) - 4000원
    private String title;
    // 응답 목록
    private List<CartItemOptionAnswerResponse> answers;
}
