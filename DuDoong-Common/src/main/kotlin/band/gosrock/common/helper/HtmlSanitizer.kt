package band.gosrock.common.helper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist

/**
 * 본문 HTML sanitize (XSS 방지) 공통 기준.
 * v2 리치 에디터([baseSafelist] 기반, api v2 의 V2HtmlSanitizer)와 v1 공연 상세 본문([sanitizeV1EventContent])이 같은 기준에서 출발한다.
 */
object HtmlSanitizer {

    /** jsoup [Safelist.relaxed]: script / style / iframe / on* 이벤트 속성 / javascript: 링크 제거. 호출마다 새 인스턴스 */
    fun baseSafelist(): Safelist = Safelist.relaxed()

    private val OUTPUT = Document.OutputSettings().prettyPrint(false)

    /**
     * v1 공연 상세 본문 (v1 호스트 어드민·운영 어드민 수정). v1 에디터(toast-ui)는 HTML 을 저장하므로
     * [baseSafelist] 에 에디터 서식을 더 허용한다: 취소선·구분선, 글자색(span style 은 color 값만), 코드 블록 언어,
     * 링크 target(rel="noopener noreferrer" 강제), 체크리스트 class. 기존 본문의 http 이미지도 남긴다.
     */
    fun sanitizeV1EventContent(content: String): String {
        val cleaned = Jsoup.clean(content, "", V1_EVENT_CONTENT, OUTPUT)
        val body = Jsoup.parseBodyFragment(cleaned).apply { outputSettings(OUTPUT) }.body()
        body.select("span[style]").forEach { if (!COLOR_STYLE.matches(it.attr("style"))) it.removeAttr("style") }
        body.select("li[class]").forEach { it.keepClasses { name -> name in TASK_LIST_CLASSES } }
        body.select("code[class]").forEach { it.keepClasses { name -> LANGUAGE_CLASS.matches(name) } }
        return body.html()
    }

    private fun Element.keepClasses(keep: (String) -> Boolean) {
        val kept = classNames().filter(keep)
        if (kept.isEmpty()) removeAttr("class") else attr("class", kept.joinToString(" "))
    }

    private val V1_EVENT_CONTENT: Safelist = baseSafelist()
        .addTags("del", "s", "hr")
        .addAttributes("span", "style")
        .addAttributes("a", "target")
        .addEnforcedAttribute("a", "rel", "noopener noreferrer")
        .addAttributes("pre", "data-language")
        .addAttributes("code", "class", "data-language")
        .addAttributes("li", "class")

    private val COLOR_STYLE = Regex("^\\s*color\\s*:\\s*(#[0-9a-fA-F]{3,8}|rgba?\\([0-9.,\\s%]+\\))\\s*;?\\s*$")
    private val LANGUAGE_CLASS = Regex("language-[A-Za-z0-9_+-]{1,30}")
    private val TASK_LIST_CLASSES = setOf("task-list-item", "checked")
}
