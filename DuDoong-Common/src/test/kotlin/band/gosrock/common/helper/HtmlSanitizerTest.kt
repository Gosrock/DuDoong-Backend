package band.gosrock.common.helper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("v1 공연 상세 본문 sanitize (#761)")
class HtmlSanitizerTest {

    private fun sanitize(html: String) = HtmlSanitizer.sanitizeV1EventContent(html)

    @Test
    fun `위험 태그와 속성은 제거된다`() {
        val out = sanitize(
            "<p>본문<script>alert(1)</script><img src=\"https://a.example.com/a.png\" onerror=\"alert(2)\">" +
                "<a href=\"javascript:alert(3)\">x</a></p><iframe src=\"https://evil.example.com\"></iframe><svg onload=\"alert(4)\"></svg>",
        )
        listOf("<script", "alert(1)", "onerror", "javascript:", "<iframe", "<svg", "onload").forEach {
            assertFalse(out.contains(it), "제거 누락($it): $out")
        }
    }

    @Test
    fun `글자색 style 만 남기고 다른 style 은 지운다`() {
        val out = sanitize("<span style=\"color: #ff0000\">빨강</span><span style=\"position:fixed;top:0\">덮기</span><span style=\"color: red; background: url(x)\">x</span>")
        assertTrue(out.contains("<span style=\"color: #ff0000\">빨강</span>"), out)
        assertTrue(out.contains("<span>덮기</span>"), out)
        assertFalse(out.contains("position") || out.contains("url(x)"), out)
    }

    @Test
    fun `에디터 서식 - 취소선, 구분선, http 이미지`() {
        val out = sanitize("<p><del>취소</del><s>취소2</s></p><hr><img src=\"http://old.example.com/a.png\">")
        listOf("<del>취소</del>", "<s>취소2</s>", "<hr>", "src=\"http://old.example.com/a.png\"").forEach {
            assertTrue(out.contains(it), "서식 손실($it): $out")
        }
    }

    @Test
    fun `코드 블록은 data-language 와 language- class 만 남긴다`() {
        val out = sanitize("<pre data-language=\"kotlin\"><code class=\"language-kotlin evil\" data-language=\"kotlin\" onclick=\"x\">val a = 1</code></pre><code class=\"evil\">b</code>")
        assertTrue(out.contains("<pre data-language=\"kotlin\">"), out)
        assertTrue(out.contains("<code class=\"language-kotlin\" data-language=\"kotlin\">"), out)
        assertTrue(out.contains("<code>b</code>"), out)
        assertFalse(out.contains("evil") || out.contains("onclick"), out)
    }

    @Test
    fun `링크 target 은 남기고 rel 을 강제한다`() {
        val out = sanitize("<a href=\"https://dudoong.com\" target=\"_blank\" rel=\"opener\">두둥</a>")
        assertEquals("<a href=\"https://dudoong.com\" target=\"_blank\" rel=\"noopener noreferrer\">두둥</a>", out)
    }

    @Test
    fun `체크리스트 class 는 task-list-item, checked 만 남긴다`() {
        val out = sanitize("<ul><li class=\"task-list-item checked evil\">a</li><li class=\"evil\">b</li></ul>")
        assertTrue(out.contains("<li class=\"task-list-item checked\">a</li>"), out)
        assertTrue(out.contains("<li>b</li>"), out)
    }
}
