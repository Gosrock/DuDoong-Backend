package band.gosrock.api.user.controller


import band.gosrock.api.user.service.MarketingUserUseCase
import band.gosrock.api.user.service.ReadUserUseCase
import band.gosrock.domain.common.vo.UserInfoVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/v1/users")
@SecurityRequirement(name = "access-token")
@Tag(name = "2. [유저]")
class UserController(
    private val readUserUseCase: ReadUserUseCase,
    private val marketingUserUseCase: MarketingUserUseCase,
) {

    @Operation(summary = "내 유저 정보를 불러 옵니다.")
    @GetMapping("/me")
    fun getMyUserInfo(): UserInfoVo {
        return readUserUseCase.execute()
    }

    @Operation(summary = "메일 동의 여부를 토글링 합니다")
    @PatchMapping("/me/mail")
    fun toggleMailReceiveAgree(): UserInfoVo {
        return marketingUserUseCase.toggleMailAgree()
    }

    @Operation(summary = "마케팅 동의 여부를 토글링 합니다")
    @PatchMapping("/me/marketing")
    fun toggleMarketingAgree(): UserInfoVo {
        return marketingUserUseCase.toggleMarketAgree()
    }
}
