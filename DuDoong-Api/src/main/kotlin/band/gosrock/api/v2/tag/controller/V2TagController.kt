package band.gosrock.api.v2.tag.controller

import band.gosrock.api.v2.tag.dto.V2TagGroupResponse
import band.gosrock.api.v2.tag.usecase.V2ReadTagsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "v2. 공연 준비")
@RestController
@RequestMapping("/api/v2")
class V2TagController(
    private val readTagsUseCase: V2ReadTagsUseCase,
) {
    @Operation(summary = "[E-11] 공연 태그 목록, 분류별 묶음 (비로그인 허용)")
    @GetMapping("/tags")
    fun getTags(): List<V2TagGroupResponse> = readTagsUseCase.execute()
}
