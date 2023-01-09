package band.gosrock.api.cart.model.dto;

import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AddCartOptionAnswerDto {

    @Schema(description = "상품에 대한 옵션 그룹 아이디")
    @NotNull
    private final Long optionGroupId;
    @Schema(description = "옵션 아이디")
    @NotNull
    private final Long optionId;

    @Schema(description = "옵션 그룹에 대한 응답/ T/F면 예,아니오 ,서술형이면 서술응답",defaultValue = "네")
    @NotBlank
    private final String answer;

    public CartOptionAnswer toCartOptionAnswer(){
        return CartOptionAnswer.builder().optionId(optionId).optionGroupId(optionGroupId).answer(answer).build();
    }

}
