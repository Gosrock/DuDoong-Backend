package band.gosrock.helper

import band.gosrock.common.annotation.Helper
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.settlement.domain.EventSettlement
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.dto.SettlementPDFDto
import band.gosrock.infrastructure.config.pdf.PdfRender
import band.gosrock.infrastructure.config.s3.S3PrivateFileService
import com.fasterxml.jackson.databind.ObjectMapper
import com.lowagie.text.DocumentException
import java.io.IOException
import java.time.LocalDateTime
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Helper
class SettlementPdfHelper(
    private val pdfRender: PdfRender,
    private val objectMapper: ObjectMapper,
    private val templateEngine: SpringTemplateEngine,
    private val s3PrivateFileUploadService: S3PrivateFileService,
) {

    @Throws(DocumentException::class, IOException::class)
    fun uploadPdfToS3(event: Event, eventSettlement: EventSettlement, masterUser: User) {
        val settlementPDFDto = getSettlementPDFDto(event, masterUser, eventSettlement)
        // 정산 관련 타임리프 파일.
        val html = templateEngine.process("settlement", getPdfHtmlContext(settlementPDFDto))
        val outputStream = pdfRender.generatePdfFromHtml(html)
        s3PrivateFileUploadService.eventSettlementPdfUpload(event.id!!, outputStream)
    }

    private fun getPdfHtmlContext(settlementPDFDto: SettlementPDFDto): Context {
        @Suppress("UNCHECKED_CAST")
        val result = objectMapper.convertValue(settlementPDFDto, Map::class.java) as Map<String, Any>
        val context = Context(null, result)
        context.setVariable("settlementAt", settlementPDFDto.settlementAt)
        context.setVariable("now", settlementPDFDto.now)
        return context
    }

    private fun getSettlementPDFDto(
        event: Event,
        masterUser: User,
        eventSettlement: EventSettlement,
    ): SettlementPDFDto =
        SettlementPDFDto(
            eventTitle = event.eventBasic!!.name!!,
            hostName = masterUser.profile!!.name!!,
            settlementAt = event.getEndAt()!!.plusDays(6L),
            dudoongTicketAmount = eventSettlement.dudoongAmount.toString(),
            pgTicketAmount = eventSettlement.paymentAmount.toString(),
            totalAmount = eventSettlement.totalSalesAmount.toString(),
            // 초기 두둥 자체 수수료 없음.
            dudoongFee = Money.ZERO.toString(),
            pgFee = eventSettlement.pgFee.toString(),
            totalFee = eventSettlement.pgFee.toString(),
            totalFeeVat = eventSettlement.pgFeeVat.toString(),
            totalSettlement = eventSettlement.totalAmount.toString(),
            now = LocalDateTime.now(),
        )
}
