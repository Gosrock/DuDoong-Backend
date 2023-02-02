package band.gosrock.api.comment.controller;


import band.gosrock.api.comment.model.request.CreateCommentRequest;
import band.gosrock.api.comment.model.response.CreateCommentResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentCountResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentListResponse;
import band.gosrock.api.comment.model.response.RetrieveRandomCommentResponse;
import band.gosrock.api.comment.service.CreateCommentUseCase;
import band.gosrock.api.comment.service.DeleteCommentUseCase;
import band.gosrock.api.comment.service.RetrieveCommentCountUseCase;
import band.gosrock.api.comment.service.RetrieveCommentUseCase;
import band.gosrock.api.comment.service.RetrieveRandomCommentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "응원톡 컨트롤러")
@RestController
@RequestMapping("/v1/events/{eventId}/comments")
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

    @Operation(summary = "응원글을 조회합니다.")
    @GetMapping
    public RetrieveCommentListResponse getComments(
            @PathVariable Long eventId, @RequestParam(required = false) Long lastId) {
        return retrieveCommentUseCase.execute(eventId, lastId);
    }

    @Operation(summary = "[어드민 기능] 응원글을 삭제합니다.")
    @DeleteMapping(value = "/{commentId}")
    public void deleteComment(@PathVariable Long eventId, @PathVariable Long commentId) {
        deleteCommentUseCase.execute(eventId, commentId);
    }

    @Operation(summary = "응원글 개수를 카운팅합니다.")
    @GetMapping(value = "/counts")
    public RetrieveCommentCountResponse getCommentCounts(@PathVariable Long eventId) {
        return retrieveCommentCountUseCase.execute(eventId);
    }

    @Operation(summary = "응원글을 랜덤으로 뽑아옵니다.")
    @GetMapping(value = "/random")
    public RetrieveRandomCommentResponse getRandomComment(@PathVariable Long eventId) {
        return retrieveRandomCommentUseCase.execute(eventId);
    }
}
