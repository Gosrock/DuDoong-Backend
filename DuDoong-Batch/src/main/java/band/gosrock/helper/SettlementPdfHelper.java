package band.gosrock.helper;


import band.gosrock.common.annotation.Helper;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.dto.SettlementPDFDto;
import band.gosrock.infrastructure.config.pdf.PdfRender;
import band.gosrock.infrastructure.config.s3.S3PrivateFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Helper
@RequiredArgsConstructor
public class SettlementPdfHelper {
    private final PdfRender pdfRender;

    private final ObjectMapper objectMapper;

    private final SpringTemplateEngine templateEngine;

    private final S3PrivateFileService s3PrivateFileUploadService;

    public void uploadPdfToS3(Event event, EventSettlement eventSettlement, User masterUser)
            throws DocumentException, IOException {
        SettlementPDFDto settlementPDFDto = getSettlementPDFDto(event, masterUser, eventSettlement);
        // 정산 관련 타임리프 파일.
        String html = templateEngine.process("settlement", getPdfHtmlContext(settlementPDFDto));
        ByteArrayOutputStream outputStream = pdfRender.generatePdfFromHtml(html);
        s3PrivateFileUploadService.eventSettlementPdfUpload(event.getId(), outputStream);
    }

    private Context getPdfHtmlContext(SettlementPDFDto settlementPDFDto) {
        Map result = objectMapper.convertValue(settlementPDFDto, Map.class);

        Context context = new Context(null, result);
        context.setVariable("settlementAt", settlementPDFDto.getSettlementAt());
        context.setVariable("now", settlementPDFDto.getNow());
        return context;
    }

    private SettlementPDFDto getSettlementPDFDto(
            Event event, User masterUser, EventSettlement eventSettlement) {
        return SettlementPDFDto.builder()
                .eventTitle(event.getEventBasic().getName())
                .hostName(masterUser.getProfile().getName())
                .settlementAt(event.getEndAt().plusDays(6L))
                .dudoongTicketAmount(eventSettlement.getDudoongAmount().toString())
                .pgTicketAmount(eventSettlement.getPaymentAmount().toString())
                .totalAmount(eventSettlement.getTotalSalesAmount().toString())
                // 초기 두둥 자체 수수료 없음.
                .dudoongFee(Money.ZERO.toString())
                .pgFee(eventSettlement.getPgFee().toString())
                .totalFee(eventSettlement.getPgFee().toString())
                .totalFeeVat(eventSettlement.getPgFeeVat().toString())
                .totalSettlement(eventSettlement.getTotalAmount().toString())
                .now(LocalDateTime.now())
                .build();
    }
}
