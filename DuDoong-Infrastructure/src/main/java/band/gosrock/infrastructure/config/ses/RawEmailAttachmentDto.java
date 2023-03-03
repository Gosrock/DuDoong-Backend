package band.gosrock.infrastructure.config.ses;


import lombok.Builder;
import lombok.Getter;

/** ses 로 raw email 보낼때 첨부파일의 내용을 지정할 수 있다. */
@Getter
@Builder
public class RawEmailAttachmentDto {

    /** file type ex : application/pdf */
    private final String type;
    /**
     * will be ByteArrayDataSource
     *
     * @see javax.mail.util.ByteArrayDataSource
     */
    private final byte[] fileBytes;
    /** 사용자가 이메일을 받았을 때 뜰 첨부파일이름. ex : 이벤트_정산서.pdf */
    private final String fileName;
}
