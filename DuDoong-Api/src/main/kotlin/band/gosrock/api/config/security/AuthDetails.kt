package band.gosrock.api.config.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AuthDetails(
    private val userId: String,
    private val role: String,
    val isAdmin: Boolean = false,
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        setOf(SimpleGrantedAuthority("ROLE_$role"))

    override fun getPassword(): String? = null

    override fun getUsername(): String = userId

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
