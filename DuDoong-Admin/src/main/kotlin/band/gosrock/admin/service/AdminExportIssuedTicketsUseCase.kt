package band.gosrock.admin.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.domain.Option
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminExportIssuedTicketsUseCase(
    private val issuedTicketRepository: IssuedTicketRepository,
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val adminExcelService: AdminExcelService,
    private val adminAuthValidator: AdminAuthValidator,
    private val adminGetIssuedTicketsUseCase: AdminGetIssuedTicketsUseCase,
) {

    fun execute(userId: Long, eventId: Long): ByteArray {
        adminAuthValidator.validateAdminOrAbove(userId)

        // 발급 티켓 엔티티 조회
        val issuedTickets = issuedTicketRepository.findAllByEventId(eventId)

        // 발급 티켓 응답 DTO 변환
        val ticketResponses = adminGetIssuedTicketsUseCase.executeAll(userId, eventId)

        // 이벤트의 옵션 그룹 → 옵션 목록 조회
        val optionGroups = optionGroupAdaptor.findAllByEventId(eventId)
        val options: List<Option> = optionGroups.flatMap { it.options }

        return adminExcelService.generateIssuedTicketsExcelWithOptions(
            tickets = ticketResponses,
            options = options,
            issuedTickets = issuedTickets,
        )
    }
}
