package band.gosrock.domain.domains.user.domain

enum class AccountRole(val value: String) {
    USER("USER"),
    MANAGER("MANAGER"),
    ADMIN("ADMIN"),
    SUPER_ADMIN("SUPER_ADMIN"),
}
