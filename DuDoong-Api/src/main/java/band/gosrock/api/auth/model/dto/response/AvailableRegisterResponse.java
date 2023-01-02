package band.gosrock.api.auth.model.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableRegisterResponse {

    @Schema(description = "회원가입을 했던 유저인지에 대한 여부 , oauth 요청을 통해 처음 회원가입한경우 false임")
    private Boolean canRegister;
}
