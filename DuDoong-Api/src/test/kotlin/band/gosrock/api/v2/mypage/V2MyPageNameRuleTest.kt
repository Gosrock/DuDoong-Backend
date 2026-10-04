package band.gosrock.api.v2.mypage

import band.gosrock.api.user.model.dto.request.ChangeNameRequest
import band.gosrock.api.v2.mypage.dto.request.V2UpdateMeRequest
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * M-2 닉네임 요청 검증 = v1 `PATCH /v1/users/me/name`(ChangeNameRequest) 검증 (#729).
 * v2 는 null(변경 안 함)을 받으려고 `@NotBlank` 대신 같은 판정의 `@Pattern` 을 쓰므로, 같은 입력에서 통과 여부가 같아야 한다
 */
@DisplayName("v2 닉네임 규칙 = v1 이름 변경 규칙")
class V2MyPageNameRuleTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    private fun v1Valid(name: String) = validator.validate(ChangeNameRequest(name)).isEmpty()

    private fun v2Valid(name: String) = validator.validate(V2UpdateMeRequest(name = name)).isEmpty()

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("names")
    fun `같은 입력이면 v1 과 v2 의 통과 여부가 같다`(name: String) {
        assertEquals(v1Valid(name), v2Valid(name), "입력 ${name.map { "U+%04X".format(it.code) }}")
    }

    @Test
    fun `대표 입력의 기대값 (2~7자, 공백만 불가)`() {
        listOf("두둥", "고스락밴드", "abcdefg", " 두 ", "a b").forEach { assertTrue(v2Valid(it), it) }
        listOf("", " ", "a", "abcdefgh", "       ", "\t\n").forEach { assertEquals(false, v2Valid(it), it) }
    }

    @Test
    fun `null 은 v2 에서만 허용 (변경 안 함)`() {
        assertTrue(validator.validate(V2UpdateMeRequest(name = null)).isEmpty())
    }

    companion object {
        @JvmStatic
        fun names(): List<String> {
            val samples = mutableListOf(
                "", " ", "  ", "a", "ab", "두둥", "abcdefg", "abcdefgh", "가나다라마바사", "가나다라마바사아",
                " ab", "ab ", " a ", "a b", "\ta", "a\n", "\u0000\u0000", "\u0001\u0001", "\u001f\u001f", "\u007f\u007f",
                "  ", "　　", "  ", "﻿﻿", "​​", "\n\n\n", "\r\n",
                "😀", "😀😀", "😀😀😀", "😀😀😀😀", "a😀", "%_", "<b>", "' OR 1", "      a", "       a",
            )
            // 길이 0~8, U+0000~U+0040 과 몇몇 유니코드 공백의 조합
            val chars = (0x00..0x40).map { it.toChar() } + listOf(' ', ' ', ' ', ' ', '　', '가')
            chars.forEach { c -> (0..8).forEach { n -> samples += c.toString().repeat(n) } }
            chars.forEach { c -> samples += " $c" }
            return samples.distinct()
        }
    }
}
