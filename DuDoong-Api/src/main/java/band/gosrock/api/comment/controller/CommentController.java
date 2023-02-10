package band.gosrock.api.comment.controller;


import band.gosrock.api.comment.model.request.CreateCommentRequest;
import band.gosrock.api.comment.model.response.CreateCommentResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentCountResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentDTO;
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse;
import band.gosrock.api.comment.service.CreateCommentUseCase;
import band.gosrock.api.comment.service.DeleteCommentUseCase;
import band.gosrock.api.comment.service.RetrieveCommentCountUseCase;
import band.gosrock.api.comment.service.RetrieveCommentUseCase;
import band.gosrock.api.comment.service.RetrieveRandomCommentUseCase;
import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.common.annotation.DisableSwaggerSecurity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "9. [응원톡]")
@RestController
@RequestMapping("/v1/events/{eventId}/comments")
@Validated
@RequiredArgsConstructor
public class CommentController {

    private final CreateCommentUseCase createCommentUseCase;

    private final RetrieveCommentUseCase retrieveCommentUseCase;

    private final DeleteCommentUseCase deleteCommentUseCase;

    private final RetrieveCommentCountUseCase retrieveCommentCountUseCase;

    private final RetrieveRandomCommentUseCase retrieveRandomCommentUseCase;

    @Operation(summary = "응원글을 생성합니다.")
    @PostMapping
    public CreateCommentResponse postComment(
            @RequestBody @Valid CreateCommentRequest createCommentRequest,
            @PathVariable Long eventId) {
        return createCommentUseCase.execute(eventId, createCommentRequest);
    }

    @DisableSwaggerSecurity
    @Operation(summary = "응원글을 조회합니다.")
    @GetMapping
    public SliceResponse<RetrieveCommentDTO> getComments(
            @PathVariable Long eventId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return retrieveCommentUseCase.execute(eventId, pageable);
    }

    @Operation(summary = "[어드민 기능] 응원글을 삭제합니다.")
    @DeleteMapping(value = "/{commentId}")
    public void deleteComment(@PathVariable Long eventId, @PathVariable Long commentId) {
        deleteCommentUseCase.execute(eventId, commentId);
    }

    @DisableSwaggerSecurity
    @Operation(summary = "응원글 개수를 카운팅합니다.")
    @GetMapping(value = "/counts")
    public RetrieveCommentCountResponse getCommentCounts(@PathVariable Long eventId) {
        return retrieveCommentCountUseCase.execute(eventId);
    }

    @DisableSwaggerSecurity
    @Operation(summary = "응원글을 랜덤으로 뽑아옵니다.")
    @GetMapping(value = "/random")
    public RetrieveRandomCommentResponse getRandomComment(
            @PathVariable Long eventId,
            @RequestParam @Min(value = 1L, message = "limit 값은 0보다 커야 합니다.") Long limit) {
        return retrieveRandomCommentUseCase.execute(eventId, limit);
    }
}
