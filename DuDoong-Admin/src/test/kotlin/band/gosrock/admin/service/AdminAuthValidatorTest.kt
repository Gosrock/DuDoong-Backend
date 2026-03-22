package band.gosrock.admin.service

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminAuthValidator")
class AdminAuthValidatorTest {

    @Mock
    private lateinit var userAdaptor: UserAdaptor

    private lateinit var adminAuthValidator: AdminAuthValidator

    @BeforeEach
    fun setUp() {
        adminAuthValidator = AdminAuthValidator(userAdaptor)
    }

    private fun createUser(userId: Long, role: AccountRole): User {
        val user = User()
        ReflectionTestUtils.setField(user, "id", userId)
        ReflectionTestUtils.setField(user, "accountRole", role)
        return user
    }

    @Nested
    @DisplayName("validateAdminOrAbove")
    inner class ValidateAdminOrAboveTest {

        @Test
        @DisplayName("USER 역할이면 예외 발생")
        fun userRoleDenied() {
            val user = createUser(1L, AccountRole.USER)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            assertThrows(DuDoongCodeException::class.java) {
                adminAuthValidator.validateAdminOrAbove(1L)
            }
        }

        @Test
        @DisplayName("MANAGER 역할이면 예외 발생")
        fun managerRoleDenied() {
            val user = createUser(1L, AccountRole.MANAGER)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            assertThrows(DuDoongCodeException::class.java) {
                adminAuthValidator.validateAdminOrAbove(1L)
            }
        }

        @Test
        @DisplayName("ADMIN 역할이면 허용")
        fun adminRoleAllowed() {
            val user = createUser(1L, AccountRole.ADMIN)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            val result = adminAuthValidator.validateAdminOrAbove(1L)
            assertNotNull(result)
            assertEquals(AccountRole.ADMIN, result.accountRole)
        }

        @Test
        @DisplayName("SUPER_ADMIN 역할이면 허용")
        fun superAdminRoleAllowed() {
            val user = createUser(1L, AccountRole.SUPER_ADMIN)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            val result = adminAuthValidator.validateAdminOrAbove(1L)
            assertNotNull(result)
            assertEquals(AccountRole.SUPER_ADMIN, result.accountRole)
        }
    }

    @Nested
    @DisplayName("validateSuperAdmin")
    inner class ValidateSuperAdminTest {

        @Test
        @DisplayName("USER 역할이면 예외 발생")
        fun userRoleDenied() {
            val user = createUser(1L, AccountRole.USER)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            assertThrows(DuDoongCodeException::class.java) {
                adminAuthValidator.validateSuperAdmin(1L)
            }
        }

        @Test
        @DisplayName("MANAGER 역할이면 예외 발생")
        fun managerRoleDenied() {
            val user = createUser(1L, AccountRole.MANAGER)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            assertThrows(DuDoongCodeException::class.java) {
                adminAuthValidator.validateSuperAdmin(1L)
            }
        }

        @Test
        @DisplayName("ADMIN 역할이면 예외 발생")
        fun adminRoleDenied() {
            val user = createUser(1L, AccountRole.ADMIN)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            assertThrows(DuDoongCodeException::class.java) {
                adminAuthValidator.validateSuperAdmin(1L)
            }
        }

        @Test
        @DisplayName("SUPER_ADMIN 역할이면 허용")
        fun superAdminRoleAllowed() {
            val user = createUser(1L, AccountRole.SUPER_ADMIN)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            val result = adminAuthValidator.validateSuperAdmin(1L)
            assertNotNull(result)
            assertEquals(AccountRole.SUPER_ADMIN, result.accountRole)
        }
    }
}
