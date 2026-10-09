package band.gosrock.api.v2.common

import band.gosrock.common.helper.HtmlSanitizer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * v2 리치 에디터 HTML sanitize (XSS 방지). 공통 기준 [HtmlSanitizer.baseSafelist] (jsoup [Safelist.relaxed]) 기반:
 * script / style / iframe / on* 이벤트 속성 / javascript: 링크 제거, 이미지 src 는 https 만 허용.
 * v1 공연 상세 본문은 [HtmlSanitizer.sanitizeV1EventContent] 를 쓴다.
 */
object V2HtmlSanitizer {
    private val SAFELIST: Safelist = HtmlSanitizer.baseSafelist()
        .removeProtocols("img", "src", "http")

    private val OUTPUT = Document.OutputSettings().prettyPrint(false)

    fun sanitize(html: String): String = Jsoup.clean(html, "", SAFELIST, OUTPUT)
}
