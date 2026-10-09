package band.gosrock.api.event.model

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * v1 공연 상세 본문 sanitize (XSS 방지).
 * v1 어드민 에디터(toast-ui)는 HTML 을 저장하므로 v2 와 같은 jsoup [Safelist.relaxed] 기준에
 * 에디터가 쓰는 태그(취소선·구분선)와 글자색(span style 의 color 만)을 더 허용한다.
 */
object EventContentSanitizer {
    private val SAFELIST: Safelist = Safelist.relaxed()
        .removeProtocols("img", "src", "http")
        .addTags("del", "s", "hr")
        .addAttributes("span", "style")
        .addAttributes("li", "class", "data-task", "data-task-checked")

    private val OUTPUT = Document.OutputSettings().prettyPrint(false)

    private val COLOR_STYLE = Regex("^\\s*color\\s*:\\s*(#[0-9a-fA-F]{3,8}|rgba?\\([0-9.,\\s%]+\\))\\s*;?\\s*$")

    fun sanitize(content: String): String {
        val cleaned = Jsoup.clean(content, "", SAFELIST, OUTPUT)
        if (!cleaned.contains("style=")) return cleaned
        val body = Jsoup.parseBodyFragment(cleaned).apply { outputSettings(OUTPUT) }.body()
        body.select("span[style]")
            .filterNot { COLOR_STYLE.matches(it.attr("style")) }
            .forEach { it.removeAttr("style") }
        return body.html()
    }
}
