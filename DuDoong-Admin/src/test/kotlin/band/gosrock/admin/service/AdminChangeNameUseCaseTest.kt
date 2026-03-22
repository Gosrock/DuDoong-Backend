package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminChangeNameRequest
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
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
@DisplayName("AdminChangeNameUseCase")
class AdminChangeNameUseCaseTest {

    @Mock
    private lateinit var userAdaptor: UserAdaptor

    private lateinit var adminAuthValidator: AdminAuthValidator
    private lateinit var adminChangeNameUseCase: AdminChangeNameUseCase

    @BeforeEach
    fun setUp() {
        adminAuthValidator = AdminAuthValidator(userAdaptor)
        adminChangeNameUseCase = AdminChangeNameUseCase(adminAuthValidator, userAdaptor)
    }

    private fun createUser(userId: Long, role: AccountRole, name: String = "기존이름"): User {
        val user = User(profile = Profile(name = name))
        ReflectionTestUtils.setField(user, "id", userId)
        ReflectionTestUtils.setField(user, "accountRole", role)
        return user
    }

    @Nested
    @DisplayName("execute")
    inner class ExecuteTest {

        @Test
        @DisplayName("ADMIN이 유저 이름을 변경하면 성공한다")
        fun adminCanChangeName() {
            val admin = createUser(1L, AccountRole.ADMIN)
            val target = createUser(2L, AccountRole.USER, "기존이름")
            `when`(userAdaptor.queryUser(1L)).thenReturn(admin)
            `when`(userAdaptor.queryUser(2L)).thenReturn(target)

            val request = AdminChangeNameRequest(name = "새이름입니다")
            adminChangeNameUseCase.execute(1L, 2L, request)

            assertEquals("새이름입니다", target.profile?.name)
        }

        @Test
        @DisplayName("SUPER_ADMIN이 유저 이름을 변경하면 성공한다")
        fun superAdminCanChangeName() {
            val superAdmin = createUser(1L, AccountRole.SUPER_ADMIN)
            val target = createUser(2L, AccountRole.USER, "기존이름")
            `when`(userAdaptor.queryUser(1L)).thenReturn(superAdmin)
            `when`(userAdaptor.queryUser(2L)).thenReturn(target)

            val request = AdminChangeNameRequest(name = "변경됨")
            adminChangeNameUseCase.execute(1L, 2L, request)

            assertEquals("변경됨", target.profile?.name)
        }

        @Test
        @DisplayName("USER 역할이면 예외가 발생한다")
        fun userRoleDenied() {
            val user = createUser(1L, AccountRole.USER)
            `when`(userAdaptor.queryUser(1L)).thenReturn(user)

            val request = AdminChangeNameRequest(name = "새이름입니다")
            assertThrows(DuDoongCodeException::class.java) {
                adminChangeNameUseCase.execute(1L, 2L, request)
            }
        }
    }
}
