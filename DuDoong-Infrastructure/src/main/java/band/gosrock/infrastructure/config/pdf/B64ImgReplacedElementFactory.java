package band.gosrock.infrastructure.config.pdf;


import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

/**
 * 이미지 가져오는 방식수정
 * https://www.tothenew.com/blog/using-data-urls-for-embedding-images-in-flying-saucer-generated-pdfs/
 */
@Component
public class B64ImgReplacedElementFactory implements ReplacedElementFactory {

    public ReplacedElement createReplacedElement(
            LayoutContext c, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }
        String nodeName = e.getNodeName();
        if (nodeName.equals("img")) {
            String attribute = e.getAttribute("src");
            FSImage fsImage;
            try {
                fsImage = buildImage(attribute, uac);
            } catch (BadElementException e1) {
                fsImage = null;
            } catch (IOException e1) {
                fsImage = null;
            }
            if (fsImage != null) {
                if (cssWidth != -1 || cssHeight != -1) {
                    fsImage.scale(cssWidth, cssHeight);
                }
                return new ITextImageElement(fsImage);
            }
        }
        return null;
    }

    protected FSImage buildImage(String srcAttr, UserAgentCallback uac)
            throws IOException, BadElementException {
        FSImage fsImage;
        if (srcAttr.startsWith("data:image/")) {
            String b64encoded =
                    srcAttr.substring(
                            srcAttr.indexOf("base64,") + "base64,".length(), srcAttr.length());

            byte[] decodedBytes = Base64.getDecoder().decode(b64encoded);
            fsImage = new ITextFSImage(Image.getInstance(decodedBytes));
        } else {
            fsImage = uac.getImageResource(srcAttr).getImage();
        }
        return fsImage;
    }

    public void remove(Element e) {}

    public void reset() {}

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {}
}
