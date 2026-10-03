package band.gosrock.api.v2.common

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * v2 리치 에디터 HTML sanitize (XSS 방지). jsoup [Safelist.relaxed] 기반:
 * script / style / iframe / on* 이벤트 속성 / javascript: 링크 제거, 이미지 src 는 https 만 허용.
 * MARKDOWN 본문(v1)은 대상이 아니다.
 */
object V2HtmlSanitizer {
    private val SAFELIST: Safelist = Safelist.relaxed()
        .removeProtocols("img", "src", "http")

    private val OUTPUT = Document.OutputSettings().prettyPrint(false)

    fun sanitize(html: String): String = Jsoup.clean(html, "", SAFELIST, OUTPUT)
}
