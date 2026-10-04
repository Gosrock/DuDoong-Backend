package band.gosrock.api.v2.operation

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.usecase.V2ReadIssuedTicketsUseCase
import band.gosrock.api.v2.operation.usecase.V2ReadOrdersUseCase
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

/**
 * DEC-021 #2 (#721): 엑셀 다운로드마다 감사 로그 1건 — 누가(userId)·어느 공연(eventId)·어떤 필터(상태/입장)·몇 행.
 * 유스케이스 로거에 logback [ListAppender] 를 붙여 로그 인자를 그대로 본다 (문구가 바뀌어도 인자 순서·값이 유지되는지).
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 엑셀 다운로드 감사 로그")
class V2ExportAuditLogTest : V2OperationTestSupport() {

    private val appender = ListAppender<ILoggingEvent>()

    private val loggers = listOf(V2ReadOrdersUseCase::class.java, V2ReadIssuedTicketsUseCase::class.java)
        .map { LoggerFactory.getLogger(it) as Logger }

    @BeforeEach
    fun attach() {
        appender.start()
        loggers.forEach { it.addAppender(appender) }
    }

    @AfterEach
    fun detach() {
        loggers.forEach { it.detachAppender(appender) }
        appender.stop()
    }

    /** 이 공연의 엑셀 감사 로그 인자: [userId, eventId, 필터, 행 수] */
    private fun auditArgs(prefix: String, eventId: Long): List<List<String>> =
        appender.list
            .filter { it.level == Level.INFO && it.message.startsWith(prefix) && it.argumentArray?.getOrNull(1) == eventId }
            .map { e -> e.argumentArray.map { "$it" } }

    @Test
    fun `주문 엑셀 - userId, eventId, 상태 필터, 행 수를 남긴다`() {
        val shop = Shop()
        shop.approved(newBuyer("승인1"))
        shop.approved(newBuyer("승인2"))
        shop.order(newBuyer("대기"))

        v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export", mapOf("status" to "APPROVED")).andExpect { status { isOk() } }
        v2Get(shop.team.master, "/events/${shop.eventId}/orders/export").andExpect { status { isOk() } }

        assertEquals(
            listOf(
                listOf("${shop.team.guest.id}", "${shop.eventId}", "APPROVED", "2"),
                listOf("${shop.team.master.id}", "${shop.eventId}", "ALL", "3"),
            ),
            auditArgs("[V2 엑셀] 주문 다운로드", shop.eventId),
        )
    }

    @Test
    fun `발급 티켓 엑셀 - userId, eventId, 입장 필터, 행 수를 남긴다`() {
        val shop = Shop()
        val approved = shop.approved(newBuyer("승인1"), quantity = 2)
        checkIn(shop.team.guest, shop.eventId, shop.ticketUuids(approved)[0]).andExpect { status { isOk() } }

        v2Get(shop.team.manager, "/events/${shop.eventId}/issued-tickets/export", mapOf("entrance" to "DONE")).andExpect { status { isOk() } }
        v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isOk() } }

        assertEquals(
            listOf(
                listOf("${shop.team.manager.id}", "${shop.eventId}", "DONE", "1"),
                listOf("${shop.team.guest.id}", "${shop.eventId}", "ALL", "2"),
            ),
            auditArgs("[V2 엑셀] 발급 티켓 다운로드", shop.eventId),
        )
    }

    @Test
    fun `권한 없음(403)으로 거절된 다운로드는 감사 로그를 남기지 않는다`() {
        val shop = Shop()
        shop.approved(newBuyer())

        v2Get(shop.team.outsider, "/events/${shop.eventId}/orders/export").andExpect { status { isForbidden() } }
        v2Get(shop.team.outsider, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isForbidden() } }

        assertEquals(emptyList<List<String>>(), auditArgs("[V2 엑셀]", shop.eventId))
    }
}
