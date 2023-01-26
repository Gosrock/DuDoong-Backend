package band.gosrock.api.comment.controller;


import band.gosrock.api.comment.model.request.CreateCommentRequest;
import band.gosrock.api.comment.model.response.CreateCommentResponse;
import band.gosrock.api.comment.model.response.RetrieveCommentListResponse;
import band.gosrock.api.comment.service.CreateCommentUseCase;
import band.gosrock.api.comment.service.RetrieveCommentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/v1/event/{eventId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CreateCommentUseCase createCommentUseCase;

    private final RetrieveCommentUseCase retrieveCommentUseCase;

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
}
