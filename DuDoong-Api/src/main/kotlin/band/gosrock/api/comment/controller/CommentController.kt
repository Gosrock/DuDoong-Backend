package band.gosrock.api.comment.controller

import band.gosrock.api.comment.model.request.CreateCommentRequest
import band.gosrock.api.comment.model.response.CreateCommentResponse
import band.gosrock.api.comment.model.response.RetrieveCommentCountResponse
import band.gosrock.api.comment.model.response.RetrieveCommentDTO
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse
import band.gosrock.api.comment.service.CreateCommentUseCase
import band.gosrock.api.comment.service.DeleteCommentUseCase
import band.gosrock.api.comment.service.RetrieveCommentCountUseCase
import band.gosrock.api.comment.service.RetrieveCommentUseCase
import band.gosrock.api.comment.service.RetrieveRandomCommentUseCase
import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.common.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "9. [응원톡]")
@RestController
@RequestMapping("/v1/events/{eventId}/comments")
@Validated
class CommentController(
    private val createCommentUseCase: CreateCommentUseCase,
    private val retrieveCommentUseCase: RetrieveCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val retrieveCommentCountUseCase: RetrieveCommentCountUseCase,
    private val retrieveRandomCommentUseCase: RetrieveRandomCommentUseCase,
) {
    @Operation(summary = "응원글을 생성합니다.")
    @PostMapping
    fun postComment(
        @RequestBody @Valid createCommentRequest: CreateCommentRequest,
        @PathVariable eventId: Long,
    ): CreateCommentResponse = createCommentUseCase.execute(eventId, createCommentRequest)

    @DisableSwaggerSecurity
    @Operation(summary = "응원글을 조회합니다.")
    @GetMapping
    fun getComments(
        @PathVariable eventId: Long,
        @ParameterObject @PageableDefault(size = 10) pageable: Pageable,
    ): SliceResponse<RetrieveCommentDTO> = retrieveCommentUseCase.execute(eventId, pageable)

    @Operation(summary = "[어드민 기능] 응원글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable eventId: Long,
        @PathVariable commentId: Long,
    ): Unit = deleteCommentUseCase.execute(eventId, commentId)

    @DisableSwaggerSecurity
    @Operation(summary = "응원글 개수를 카운팅합니다.")
    @GetMapping("/counts")
    fun getCommentCounts(
        @PathVariable eventId: Long,
    ): RetrieveCommentCountResponse = retrieveCommentCountUseCase.execute(eventId)

    @DisableSwaggerSecurity
    @Operation(summary = "응원글을 랜덤으로 뽑아옵니다.")
    @GetMapping("/random")
    fun getRandomComment(
        @PathVariable eventId: Long,
        @RequestParam @Min(value = 1L, message = "limit 값은 0보다 커야 합니다.") limit: Long,
    ): RetrieveRandomCommentResponse = retrieveRandomCommentUseCase.execute(eventId, limit)
}
