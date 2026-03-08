package band.gosrock.infrastructure.config.pdf

import com.lowagie.text.BadElementException
import com.lowagie.text.Image
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.xhtmlrenderer.extend.FSImage
import org.xhtmlrenderer.extend.ReplacedElement
import org.xhtmlrenderer.extend.ReplacedElementFactory
import org.xhtmlrenderer.extend.UserAgentCallback
import org.xhtmlrenderer.layout.LayoutContext
import org.xhtmlrenderer.pdf.ITextFSImage
import org.xhtmlrenderer.pdf.ITextImageElement
import org.xhtmlrenderer.render.BlockBox
import org.xhtmlrenderer.simple.extend.FormSubmissionListener
import java.io.IOException
import java.util.Base64

/**
 * 이미지 가져오는 방식수정
 * https://www.tothenew.com/blog/using-data-urls-for-embedding-images-in-flying-saucer-generated-pdfs/
 */
@Component
class B64ImgReplacedElementFactory : ReplacedElementFactory {

    override fun createReplacedElement(
        c: LayoutContext,
        box: BlockBox,
        uac: UserAgentCallback,
        cssWidth: Int,
        cssHeight: Int,
    ): ReplacedElement? {
        val e = box.element ?: return null
        val nodeName = e.nodeName
        if (nodeName == "img") {
            val attribute = e.getAttribute("src")
            val fsImage: FSImage? = try {
                buildImage(attribute, uac)
            } catch (e1: BadElementException) {
                null
            } catch (e1: IOException) {
                null
            }
            if (fsImage != null) {
                if (cssWidth != -1 || cssHeight != -1) {
                    fsImage.scale(cssWidth, cssHeight)
                }
                return ITextImageElement(fsImage)
            }
        }
        return null
    }

    @Throws(IOException::class, BadElementException::class)
    protected fun buildImage(srcAttr: String, uac: UserAgentCallback): FSImage {
        return if (srcAttr.startsWith("data:image/")) {
            val b64encoded = srcAttr.substring(srcAttr.indexOf("base64,") + "base64,".length)
            val decodedBytes = Base64.getDecoder().decode(b64encoded)
            ITextFSImage(Image.getInstance(decodedBytes))
        } else {
            uac.getImageResource(srcAttr).image
        }
    }

    override fun remove(e: Element) {}

    override fun reset() {}

    override fun setFormSubmissionListener(listener: FormSubmissionListener) {}
}
