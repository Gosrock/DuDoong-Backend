package band.gosrock.domain.domains.user.domain

enum class AccountState(val value: String) {
    NORMAL("NORMAL"),
    // 탈퇴한유저
    DELETED("DELETED"),
    // 영구정지
    FORBIDDEN("FORBIDDEN"),
    // 7일정지등..? 나중을위한
    SUSPENDED("SUSPENDED"),
}
