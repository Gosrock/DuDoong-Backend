package band.gosrock.domain.domain.user.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AccountState {
    NORMAL("NORMAL"),
    // 탈퇴한유저
    DELETED("DELETED"),
    // 영구정지
    FORBIDDEN("FORBIDDEN"),
    // 7일정지등..? 나중을위한
    SUSPENDED("SUSPENDED");

    private String value;
}
