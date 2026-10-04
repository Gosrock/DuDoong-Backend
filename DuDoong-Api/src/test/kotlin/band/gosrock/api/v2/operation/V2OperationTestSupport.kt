package band.gosrock.api.v2.operation

import band.gosrock.api.v2.ticket.V2TicketApiTestSupport
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import java.io.ByteArrayInputStream
import java.util.UUID
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * v2 공연 운영 통합 테스트 공통 픽스처 (#712).
 * [Shop]: 팀(마스터·매니저·일반·외부인) + 등록(OPEN)된 공연 + 옵션 2개(네/아니오 +1000, 주관식)가 붙은 두둥티켓(6000원, 승인).
 * 주문은 v1 사용자 API(카트 → 주문)로 만든다.
 */
abstract class V2OperationTestSupport : V2TicketApiTestSupport() {

    @Autowired protected lateinit var orderRepository: OrderRepository

    @Autowired protected lateinit var issuedTicketRepository: IssuedTicketRepository

    protected fun newBuyer(name: String = "구매자", phone: String = "010-1234-5678"): User =
        userRepository.save(
            User(
                profile = Profile(name = name, email = "v2op-${UUID.randomUUID()}@test.com", phoneNumber = phone, profileImage = null),
                oauthInfo = OauthInfo(OauthProvider.KAKAO, "v2op-${UUID.randomUUID()}"),
            ),
        )

    /**
     * 주의: 1인 매수 제한 4장 티켓에 한 주문 3장(제한 절반 초과)은 **H2 테스트에서만** v1 승인이 Ticket_Item_400_6 으로 실패한다.
     * v1 승인은 같은 트랜잭션 안에서 발급(별도 REQUIRES_NEW 커밋) 후 매수 제한을 세는데, H2(READ COMMITTED)는 방금 커밋된 발급분을 같이 세고
     * MySQL(REPEATABLE READ)은 트랜잭션 첫 조회 시점 스냅샷이라 세지 않는다. prod 승인 데이터로 반증됨(2026-03-15 이후 제한 절반 초과 승인 매달 성공).
     * 그래서 이 경계값은 MySQL E2E(test_42)에서 검증하고, 여기서는 2장 이하로 주문한다
     */
    protected inner class Shop(hostName: String = "고스락") {
        val team = Team(hostName)
        val eventId get() = team.eventId
        val yesNoOptionId = createOption(team.manager, team.eventId, type = "YES_NO", yesAdditionalPrice = 1000, name = "뒷풀이")
        val subjectiveOptionId = createOption(team.manager, team.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "입금자명")
        val ticketId: Long = createTicket(team.manager, team.eventId, dudoongBody(supplyCount = 20))

        init {
            putOptions(team.manager, team.eventId, ticketId, listOf(yesNoOptionId, subjectiveOptionId)).andExpect { status { isOk() } }
            setEventStatus(team.eventId, EventStatus.OPEN)
        }

        /** v1 카트 → 주문 (승인 대기) */
        fun order(buyer: User, quantity: Long = 1): String = v1Order(buyer, team.eventId, ticketId, quantity)

        /** v1 주문 + v1 승인 → 티켓 발급 */
        fun approved(buyer: User, quantity: Long = 1): String =
            order(buyer, quantity).also { v1Approve(team.master, team.eventId, it).andExpect { status { isOk() } } }

        fun ticketUuids(orderUuid: String): List<String> =
            issuedTicketRepository.findAllByOrderUuid(orderUuid).sortedBy { it.id }.map { it.uuid!! }
    }

    // ===== v2 운영 API =====

    protected fun v2Get(requester: User?, path: String, params: Map<String, String> = emptyMap()): ResultActionsDsl =
        mockMvc.get("/api/v2$path") {
            requester?.let { with(auth(it)) }
            params.forEach { (k, v) -> param(k, v) }
        }

    protected fun v2Post(requester: User?, path: String, body: Any? = null): ResultActionsDsl =
        mockMvc.post("/api/v2$path") {
            requester?.let { with(auth(it)) }
            if (body != null) {
                contentType = MediaType.APPLICATION_JSON
                content = json(body)
            }
        }

    protected fun orders(requester: User, eventId: Long, params: Map<String, String> = emptyMap()): JsonNode =
        v2Get(requester, "/events/$eventId/orders", params).andExpect { status { isOk() } }.data()

    protected fun refuse(requester: User, eventId: Long, orderUuid: String, reasonType: String?, reasonText: String? = null): ResultActionsDsl =
        v2Post(requester, "/events/$eventId/orders/$orderUuid/refuse", mapOf("reasonType" to reasonType, "reasonText" to reasonText))

    protected fun checkIn(requester: User, eventId: Long, ticketUuid: String): ResultActionsDsl =
        v2Post(requester, "/events/$eventId/check-ins", mapOf("ticketUuid" to ticketUuid))

    protected fun selfCheckIn(requester: User?, token: String, ticketUuid: String? = null): ResultActionsDsl =
        v2Post(requester, "/check-ins/self", mapOf("token" to token, "ticketUuid" to ticketUuid))

    protected fun qrToken(requester: User, eventId: Long): String =
        v2Get(requester, "/events/$eventId/check-in-qr").andExpect { status { isOk() } }.data().at("/token").asText()

    protected fun ResultActionsDsl.code(): String = body().at("/code").asText()

    protected fun ResultActionsDsl.sheet(): Sheet {
        val bytes = andReturn().response.contentAsByteArray
        return XSSFWorkbook(ByteArrayInputStream(bytes)).getSheetAt(0)
    }

    protected fun Sheet.headers(): List<String> = getRow(0).map { it.stringCellValue }

    protected fun Sheet.column(header: String): List<String> {
        val index = headers().indexOf(header)
        return (1..lastRowNum).map { getRow(it).getCell(index)?.toString().orEmpty() }
    }
}
