package band.gosrock.admin.controller

import band.gosrock.admin.model.dto.response.AdminCommentResponse
import band.gosrock.admin.service.AdminDeleteCommentUseCase
import band.gosrock.admin.service.AdminGetCommentsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/comments")
@SecurityRequirement(name = "access-token")
@Tag(name = "Admin")
class AdminCommentController(
    private val adminGetCommentsUseCase: AdminGetCommentsUseCase,
    private val adminDeleteCommentUseCase: AdminDeleteCommentUseCase,
) {

    @Operation(summary = "댓글 목록을 조회합니다.")
    @GetMapping
    fun getComments(
        @RequestParam(required = false) keyword: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AdminCommentResponse> {
        return adminGetCommentsUseCase.execute(keyword, pageable)
    }

    @Operation(summary = "댓글을 삭제합니다. (소프트 삭제)")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(@PathVariable commentId: Long) {
        adminDeleteCommentUseCase.execute(commentId)
    }
}
