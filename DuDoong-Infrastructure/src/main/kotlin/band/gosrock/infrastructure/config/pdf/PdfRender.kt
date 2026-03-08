package band.gosrock.infrastructure.config.pdf

import com.lowagie.text.DocumentException
import com.lowagie.text.pdf.BaseFont
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@Component
class PdfRender(
    private val b64ImgReplacedElementFactory: B64ImgReplacedElementFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(DocumentException::class, IOException::class)
    fun generatePdfFromHtml(html: String): ByteArrayOutputStream {
        val outputFolder = System.getProperty("user.home") + File.separator + "thymeleaf.pdf"
        val outputStream = ByteArrayOutputStream()
        log.info(outputFolder)
        val renderer = ITextRenderer()
        val sharedContext = renderer.sharedContext
        sharedContext.isPrint = true
        sharedContext.isInteractive = false
        sharedContext.setReplacedElementFactory(b64ImgReplacedElementFactory)
        sharedContext.textRenderer.setSmoothingThreshold(0f)

        renderer.fontResolver.addFont(
            ClassPathResource("/templates/NanumBarunGothic.ttf").url.toString(),
            BaseFont.IDENTITY_H,
            BaseFont.EMBEDDED,
        )
        renderer.setDocumentFromString(html)
        renderer.layout()
        renderer.createPDF(outputStream)
        outputStream.close()
        return outputStream
    }
}
