package band.gosrock.api.cart.model.dto.response;


import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCartResponse {

    // 임시값
    private final String title = "고스락 제 23회 정기공연 일반티켓 총 3매";
    // 내티켓 확인하기
    private final List<CartItemResponse> items;

    // 금액
    private final String totalPrice;
}
