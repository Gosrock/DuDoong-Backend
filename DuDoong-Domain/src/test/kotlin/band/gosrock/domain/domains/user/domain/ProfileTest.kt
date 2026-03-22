package band.gosrock.domain.domains.user.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Profile")
class ProfileTest {

    @Nested
    @DisplayName("changeName")
    inner class ChangeNameTest {

        @Test
        @DisplayName("정상적인 이름으로 변경하면 성공한다")
        fun successWithValidName() {
            val profile = Profile(name = "기존이름")
            profile.changeName("새이름입니다")
            assertEquals("새이름입니다", profile.name)
        }

        @Test
        @DisplayName("2자 이름으로 변경하면 성공한다")
        fun successWithMinLength() {
            val profile = Profile(name = "기존이름")
            profile.changeName("두글")
            assertEquals("두글", profile.name)
        }

        @Test
        @DisplayName("7자 이름으로 변경하면 성공한다")
        fun successWithMaxLength() {
            val profile = Profile(name = "기존이름")
            val name7 = "가".repeat(7)
            profile.changeName(name7)
            assertEquals(name7, profile.name)
        }

        @Test
        @DisplayName("빈 문자열이면 예외가 발생한다")
        fun failWithBlankName() {
            val profile = Profile(name = "기존이름")
            assertThrows<IllegalArgumentException> {
                profile.changeName("")
            }
        }

        @Test
        @DisplayName("공백만 있는 문자열이면 예외가 발생한다")
        fun failWithWhitespaceName() {
            val profile = Profile(name = "기존이름")
            assertThrows<IllegalArgumentException> {
                profile.changeName("   ")
            }
        }

        @Test
        @DisplayName("1자 이름이면 예외가 발생한다")
        fun failWithTooShortName() {
            val profile = Profile(name = "기존이름")
            assertThrows<IllegalArgumentException> {
                profile.changeName("가")
            }
        }

        @Test
        @DisplayName("16자 이름이면 예외가 발생한다")
        fun failWithTooLongName() {
            val profile = Profile(name = "기존이름")
            val name16 = "가".repeat(16)
            assertThrows<IllegalArgumentException> {
                profile.changeName(name16)
            }
        }
    }
}
