package band.gosrock.domain.domains.comment.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;
}
