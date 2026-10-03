package band.gosrock.api.v2.operation.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.operation.dto.response.V2CheckInQrResponse
import band.gosrock.api.v2.operation.dto.response.V2CheckInResponse
import band.gosrock.api.v2.operation.dto.response.V2EntranceStatsResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInDomainService
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInOutcome
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketQuery
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketSearch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory

/** Q-1 통계 / Q-2 호스트 스캔 / Q-4 셀프 체크인 QR (일반 멤버 이상, DEC-009) / Q-5 관객 셀프 체크인 (로그인 유저) */
@UseCase
class V2CheckInUseCase(
    private val eventAdaptor: EventAdaptor,
    private val v2CheckInDomainService: V2CheckInDomainService,
    private val v2IssuedTicketQuery: V2IssuedTicketQuery,
    private val mapper: V2OperationMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun stats(userId: Long, eventId: Long): V2EntranceStatsResponse {
        eventAdaptor.findById(eventId)
        return V2EntranceStatsResponse.of(v2IssuedTicketQuery.stats(V2IssuedTicketSearch(eventId = eventId)))
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun checkIn(userId: Long, eventId: Long, ticketUuid: String): V2CheckInResponse {
        val outcome = v2CheckInDomainService.checkIn(eventId, ticketUuid)
        log.info("[V2CheckInUseCase] 호스트 스캔 userId={} eventId={} ticketUuid={} result={}", userId, eventId, ticketUuid, outcome.result)
        return toResponse(outcome)
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun qr(userId: Long, eventId: Long): V2CheckInQrResponse {
        val token = v2CheckInDomainService.getOrCreateCheckInToken(eventId)
        return V2CheckInQrResponse(token = token, qrPath = "$QR_PATH?token=${URLEncoder.encode(token, StandardCharsets.UTF_8)}")
    }

    fun selfCheckIn(userId: Long, token: String, ticketUuid: String?): V2CheckInResponse {
        val outcome = v2CheckInDomainService.selfCheckIn(userId, token, ticketUuid)
        log.info("[V2CheckInUseCase] 셀프 체크인 userId={} ticketUuid={} result={}", userId, ticketUuid, outcome.result)
        return toResponse(outcome)
    }

    private fun toResponse(outcome: V2CheckInOutcome): V2CheckInResponse = mapper.toCheckInResponse(outcome)

    companion object {
        /** 프론트 셀프 체크인 화면 경로 (origin 은 프론트가 붙인다) */
        const val QR_PATH = "/check-in"
    }
}
