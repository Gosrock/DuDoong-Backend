package band.gosrock.api.user.controller

import band.gosrock.api.user.model.dto.request.ChangeNameRequest
import band.gosrock.api.user.service.ChangeNameUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.api.user.service.MarketingUserUseCase
import band.gosrock.api.user.service.ReadUserUseCase
import band.gosrock.domain.common.vo.UserInfoVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "access-token")
@Tag(name = "2. [유저]")
class UserController(
    private val readUserUseCase: ReadUserUseCase,
    private val marketingUserUseCase: MarketingUserUseCase,
    private val changeNameUseCase: ChangeNameUseCase,
) {

    @Operation(summary = "내 유저 정보를 불러 옵니다.")
    @GetMapping("/me")
    fun getMyUserInfo(@CurrentUserId userId: Long): UserInfoVo {
        return readUserUseCase.execute(userId)
    }

    @Operation(summary = "메일 동의 여부를 토글링 합니다")
    @PatchMapping("/me/mail")
    fun toggleMailReceiveAgree(@CurrentUserId userId: Long): UserInfoVo {
        return marketingUserUseCase.toggleMailAgree(userId)
    }

    @Operation(summary = "마케팅 동의 여부를 토글링 합니다")
    @PatchMapping("/me/marketing")
    fun toggleMarketingAgree(@CurrentUserId userId: Long): UserInfoVo {
        return marketingUserUseCase.toggleMarketAgree(userId)
    }

    @Operation(summary = "내 닉네임 변경")
    @PatchMapping("/me/name")
    fun changeMyName(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: ChangeNameRequest,
    ) {
        changeNameUseCase.execute(userId, request)
    }
}
