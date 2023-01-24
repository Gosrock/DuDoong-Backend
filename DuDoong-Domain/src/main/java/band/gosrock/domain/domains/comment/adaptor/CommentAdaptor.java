package band.gosrock.domain.domains.comment.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.comment.domain.Comment;
import band.gosrock.domain.domains.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CommentAdaptor {

    private final CommentRepository commentRepository;

    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }
}
