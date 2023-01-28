package band.gosrock.api.comment.service;

import band.gosrock.api.comment.mapper.CommentMapper;
import band.gosrock.api.common.UserUtils;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final UserUtils userUtils;

    private final CommentMapper commentMapper;


}
