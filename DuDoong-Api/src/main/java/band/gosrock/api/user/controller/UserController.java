package band.gosrock.api.user.controller;


import band.gosrock.api.user.service.MarketingUserUseCase;
import band.gosrock.api.user.service.ReadUserUseCase;
import band.gosrock.domain.common.vo.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@SecurityRequirement(name = "access-token")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "2. [유저]")
public class UserController {

    private final ReadUserUseCase readUserUseCase;

    private final MarketingUserUseCase marketingUserUseCase;

    @Operation(summary = "내 유저 정보를 불러 옵니다.")
    @GetMapping("/me")
    public UserInfoVo getMyUserInfo() {
        return readUserUseCase.execute();
    }

    @Operation(summary = "메일 동의 여부를 토글링 합니다")
    @PatchMapping("/me/mail")
    public UserInfoVo toggleMailReceiveAgree() {
        return marketingUserUseCase.toggleMailAgree();
    }

    @Operation(summary = "마케팅 동의 여부를 토글링 합니다")
    @PatchMapping("/me/marketing")
    public UserInfoVo toggleMarketingAgree() {
        return marketingUserUseCase.toggleMarketAgree();
    }
}
