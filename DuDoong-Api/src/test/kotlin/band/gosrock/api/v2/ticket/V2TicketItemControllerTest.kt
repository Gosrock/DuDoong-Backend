package band.gosrock.api.v2.ticket

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/** v2 티켓 API 통합 테스트 (#707): T-1 ~ T-6 + v1 호환 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 티켓 API")
class V2TicketItemControllerTest : V2TicketApiTestSupport() {

    @Nested
    @DisplayName("T-2 생성 / T-1 관리 목록")
    inner class Create {

        @Test
        fun `두둥티켓 생성 - 승인 강제, 계좌, 판매 전, 재고 항상 공개`() {
            val team = Team()
            val body = dudoongBody(overrides = mapOf("approvalRequired" to false, "isQuantityPublic" to false))
            postTicket(team.manager, team.eventId, body).andExpect {
                status { isOk() }
                jsonPath("$.data.payType") { value("DUDOONG") }
                jsonPath("$.data.name") { value("일반") }
                jsonPath("$.data.price") { value(6000) }
                jsonPath("$.data.supplyCount") { value(100) }
                jsonPath("$.data.remaining") { value(100) }
                jsonPath("$.data.soldCount") { value(0) }
                jsonPath("$.data.approvalRequired") { value(true) }
                jsonPath("$.data.isQuantityPublic") { value(false) }
                jsonPath("$.data.purchaseLimit") { value(4) }
                jsonPath("$.data.saleState") { value("BEFORE_SALE") }
                jsonPath("$.data.isSold") { value(false) }
                // 준비중 공연이라 아직 살 수 없다
                jsonPath("$.data.isPurchasable") { value(false) }
                jsonPath("$.data.account.bank") { value("신한은행") }
                jsonPath("$.data.account.holder") { value("고스락") }
                jsonPath("$.data.account.number") { value("110-123-456789") }
                jsonPath("$.data.options.length()") { value(0) }
            }
            // 일반 멤버도 관리 목록 조회 가능, 재고 비공개여도 remaining 이 보인다
            manage(team.guest, team.eventId).andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].remaining") { value(100) }
            }
        }

        @Test
        fun `무료 무제한 - supplyCount-remaining-purchaseLimit null, 재고 공개 꺼짐, DB 는 저장값`() {
            val team = Team()
            val saleEnd = eventStart.minusDays(1)
            val id = postTicket(
                team.manager, team.eventId,
                freeBody(supplyCount = null, overrides = mapOf("purchaseLimit" to null, "account" to account, "saleEndAt" to saleEnd.f())),
            ).andExpect {
                status { isOk() }
                jsonPath("$.data.payType") { value("FREE") }
                jsonPath("$.data.supplyCount") { value(null as Any?) }
                jsonPath("$.data.remaining") { value(null as Any?) }
                jsonPath("$.data.purchaseLimit") { value(null as Any?) }
                jsonPath("$.data.isQuantityPublic") { value(false) }
                jsonPath("$.data.approvalRequired") { value(false) }
                jsonPath("$.data.account") { value(null as Any?) }
                jsonPath("$.data.saleStartAt") { value(null as Any?) }
                jsonPath("$.data.saleEndAt") { value(saleEnd.f()) }
            }.data().at("/ticketItemId").asLong()
            val item = ticketItemRepository.findById(id).get()
            assertEquals(V2TicketItemDomainService.UNLIMITED_SUPPLY_COUNT, item.supplyCount)
            assertEquals(V2TicketItemDomainService.UNLIMITED_SUPPLY_COUNT, item.quantity)
            assertEquals(V2TicketItemDomainService.NO_PURCHASE_LIMIT, item.purchaseLimit)
            assertEquals(TicketType.FIRST_COME_FIRST_SERVED, item.type)
            assertTrue(item.isSellable!!)
        }

        @Test
        fun `티켓 없음 공연, PRICE, 계좌 누락, 가격 규칙, 입력 길이, 판매기간 역전-공연 이후는 400`() {
            val team = Team()
            val noTicketEvent = team.createEvent(hasTicket = false)
            postTicket(team.manager, noTicketEvent, dudoongBody()).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Ticket_Item_400_12") }
            }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("payType" to "PRICE")))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_11") } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("account" to null)))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_8") } }
            postTicket(team.manager, team.eventId, dudoongBody(price = 0))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_3") } }
            postTicket(team.manager, team.eventId, freeBody(overrides = mapOf("price" to 1000)))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_3") } }
            postTicket(team.manager, team.eventId, dudoongBody(name = "가".repeat(13))).andExpect { status { isBadRequest() } }
            postTicket(team.manager, team.eventId, dudoongBody(name = "  ")).andExpect { status { isBadRequest() } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("description" to "가".repeat(31)))).andExpect { status { isBadRequest() } }
            postTicket(team.manager, team.eventId, dudoongBody(supplyCount = 0)).andExpect { status { isBadRequest() } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("account" to mapOf("bank" to "", "holder" to "a", "number" to "1"))))
                .andExpect { status { isBadRequest() } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("payType" to "UNKNOWN"))).andExpect { status { isBadRequest() } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("approvalRequired" to null))).andExpect { status { isBadRequest() } }
            postTicket(
                team.manager, team.eventId,
                dudoongBody(overrides = mapOf("saleStartAt" to eventStart.minusDays(1).f(), "saleEndAt" to eventStart.minusDays(2).f())),
            ).andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_13") } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("saleEndAt" to eventStart.plusMinutes(1).f())))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_13") } }
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("saleEndAt" to LocalDateTime.now().minusDays(1).f())))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Ticket_Item_400_13") } }
            // 판매 종료 = 공연 시작은 허용
            postTicket(team.manager, team.eventId, dudoongBody(overrides = mapOf("saleEndAt" to eventStart.f()))).andExpect { status { isOk() } }
            manage(team.manager, team.eventId).andExpect { jsonPath("$.data.length()") { value(1) } }
        }

        @Test
        fun `권한 - 일반·비멤버 생성 403, 비로그인 401, 비멤버 조회 403, SUPER_ADMIN 은 생성·조회 가능, 다른 호스트 공연 IDOR 403`() {
            val team = Team()
            val other = Team("다른호스트")
            postTicket(team.guest, team.eventId, dudoongBody()).andExpect { status { isForbidden() } }
            postTicket(team.outsider, team.eventId, dudoongBody()).andExpect { status { isForbidden() } }
            mockMvc.post("/api/v2/events/${team.eventId}/ticket-items") {
                contentType = MediaType.APPLICATION_JSON
                content = json(dudoongBody())
            }.andExpect { status { isUnauthorized() } }
            mockMvc.get("/api/v2/events/${team.eventId}/ticket-items/manage").andExpect { status { isUnauthorized() } }
            manage(team.outsider, team.eventId).andExpect { status { isForbidden() } }
            // 내 호스트 매니저가 남의 공연 eventId 로 생성·조회
            postTicket(team.manager, other.eventId, dudoongBody()).andExpect { status { isForbidden() } }
            manage(team.manager, other.eventId).andExpect { status { isForbidden() } }

            val admin = superAdmin()
            postTicket(admin, team.eventId, dudoongBody()).andExpect { status { isOk() } }
            manage(admin, team.eventId).andExpect { status { isOk() }; jsonPath("$.data.length()") { value(1) } }
            // 없는 공연
            manage(team.master, 987654321L).andExpect { status { isNotFound() } }
        }

        @Test
        fun `종료된 공연에는 만들 수 없고, 등록(OPEN) 후에는 추가할 수 있다`() {
            val team = Team()
            setEventStatus(team.eventId, EventStatus.OPEN)
            postTicket(team.manager, team.eventId, dudoongBody()).andExpect {
                status { isOk() }
                jsonPath("$.data.isPurchasable") { value(true) }
            }
            setEventStatus(team.eventId, EventStatus.CLOSED)
            postTicket(team.manager, team.eventId, dudoongBody()).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_17") }
            }
        }

        @Test
        fun `v2 티켓을 만들면 체크리스트 ticket 이 충족된다`() {
            val team = Team()
            mockMvc.get("/api/v2/events/${team.eventId}/checklist") { with(auth(team.guest)) }
                .andExpect { jsonPath("$.data.ticket") { value(false) } }
            createTicket(team.manager, team.eventId)
            mockMvc.get("/api/v2/events/${team.eventId}/checklist") { with(auth(team.guest)) }
                .andExpect { jsonPath("$.data.ticket") { value(true) } }
        }
    }

    @Nested
    @DisplayName("T-3 수정")
    inner class Update {

        @Test
        fun `판매 전이면 종류·이름·가격·계좌·승인·수량 모두 수정`() {
            val team = Team()
            val id = createTicket(team.manager, team.eventId, dudoongBody(supplyCount = 100))
            patchTicket(team.manager, team.eventId, id, freeBody(name = "무료로", supplyCount = 5, approvalRequired = true)).andExpect {
                status { isOk() }
                jsonPath("$.data.payType") { value("FREE") }
                jsonPath("$.data.name") { value("무료로") }
                jsonPath("$.data.price") { value(0) }
                jsonPath("$.data.supplyCount") { value(5) }
                jsonPath("$.data.remaining") { value(5) }
                jsonPath("$.data.approvalRequired") { value(true) }
                jsonPath("$.data.account") { value(null as Any?) }
            }
            val item = ticketItemRepository.findById(id).get()
            assertEquals(TicketType.APPROVAL, item.type)
            assertEquals(null, item.accountInfo?.bankName)
        }

        @Test
        fun `판매됨이면 설명·판매기간·재고공개·매수제한·수량 증가만, 나머지 변경과 수량 감소는 400`() {
            val team = Team()
            val id = createTicket(team.manager, team.eventId, freeBody(supplyCount = 10))
            setEventStatus(team.eventId, EventStatus.OPEN)
            v1Buy(newUser("구매자"), team.master, team.eventId, id, quantity = 2, approval = false)
            assertEquals("SOLD", manageItem(team.guest, team.eventId, id).at("/saleState").asText())

            val saleStart = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)
            patchTicket(
                team.manager, team.eventId, id,
                freeBody(supplyCount = 20, overrides = mapOf("description" to "바뀐 설명", "isQuantityPublic" to false, "purchaseLimit" to 1, "saleStartAt" to saleStart.f())),
            ).andExpect {
                status { isOk() }
                jsonPath("$.data.description") { value("바뀐 설명") }
                jsonPath("$.data.isQuantityPublic") { value(false) }
                jsonPath("$.data.purchaseLimit") { value(1) }
                jsonPath("$.data.saleStartAt") { value(saleStart.f()) }
                jsonPath("$.data.supplyCount") { value(20) }
                jsonPath("$.data.remaining") { value(18) }
                jsonPath("$.data.soldCount") { value(2) }
                jsonPath("$.data.saleState") { value("SOLD") }
            }
            listOf(
                freeBody(name = "다른이름", supplyCount = 20),
                freeBody(supplyCount = 20, approvalRequired = true),
                freeBody(supplyCount = 19),
                freeBody(supplyCount = 2),
                dudoongBody(name = "무료", supplyCount = 20),
            ).forEach { body ->
                patchTicket(team.manager, team.eventId, id, body).andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("Ticket_Item_400_14") }
                }
            }
            val item = ticketItemRepository.findById(id).get()
            assertEquals("무료", item.name)
            assertEquals(20L, item.supplyCount)
            assertEquals(18L, item.quantity)
        }

        @Test
        fun `판매된 두둥티켓은 가격·계좌 변경 400 (승인 후 재고 감소)`() {
            val team = Team()
            val id = createTicket(team.manager, team.eventId, dudoongBody(supplyCount = 10))
            setEventStatus(team.eventId, EventStatus.OPEN)
            v1Buy(newUser("구매자"), team.master, team.eventId, id, approval = true)
            assertEquals(1L, manageItem(team.guest, team.eventId, id).at("/soldCount").asLong())
            patchTicket(team.manager, team.eventId, id, dudoongBody(price = 7000, supplyCount = 10))
                .andExpect { jsonPath("$.code") { value("Ticket_Item_400_14") } }
            patchTicket(team.manager, team.eventId, id, dudoongBody(supplyCount = 10, overrides = mapOf("account" to account + ("number" to "999"))))
                .andExpect { jsonPath("$.code") { value("Ticket_Item_400_14") } }
            // 같은 값(폼 전체 재전송)은 허용
            patchTicket(team.manager, team.eventId, id, dudoongBody(supplyCount = 10)).andExpect { status { isOk() } }
        }

        @Test
        fun `권한·IDOR - 일반 403, 다른 공연 티켓 id 를 내 공연 경로로 404, PRICE 로 변경 400`() {
            val team = Team()
            val other = Team("다른호스트")
            val id = createTicket(team.manager, team.eventId)
            val otherId = createTicket(other.manager, other.eventId)
            patchTicket(team.guest, team.eventId, id, dudoongBody()).andExpect { status { isForbidden() } }
            patchTicket(team.manager, other.eventId, otherId, dudoongBody()).andExpect { status { isForbidden() } }
            patchTicket(team.manager, team.eventId, otherId, dudoongBody()).andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("Ticket_Item_404_1") }
            }
            deleteTicket(team.manager, team.eventId, otherId).andExpect { status { isNotFound() } }
            suspend(team.manager, team.eventId, otherId).andExpect { status { isNotFound() } }
            resume(team.manager, team.eventId, otherId).andExpect { status { isNotFound() } }
            putOptions(team.manager, team.eventId, otherId, emptyList()).andExpect { status { isNotFound() } }
            patchTicket(team.manager, team.eventId, id, dudoongBody(overrides = mapOf("payType" to "PRICE")))
                .andExpect { jsonPath("$.code") { value("Ticket_Item_400_11") } }
            // 남의 티켓은 그대로
            assertEquals("일반", ticketItemRepository.findById(otherId).get().name)
            assertEquals(TicketItemStatus.VALID, ticketItemRepository.findById(otherId).get().ticketItemStatus)
        }
    }

    @Nested
    @DisplayName("T-4 삭제 / T-5·T-6 판매 중단·재개")
    inner class DeleteAndSuspend {

        @Test
        fun `판매 전 삭제는 남은 목록 반환, 판매된 티켓 삭제 400`() {
            val team = Team()
            val keep = createTicket(team.manager, team.eventId, freeBody(name = "남김"))
            val removed = createTicket(team.manager, team.eventId, freeBody(name = "삭제"))
            deleteTicket(team.guest, team.eventId, removed).andExpect { status { isForbidden() } }
            deleteTicket(team.manager, team.eventId, removed).andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].ticketItemId") { value(keep) }
            }
            assertEquals(TicketItemStatus.DELETED, ticketItemRepository.findById(removed).get().ticketItemStatus)
            deleteTicket(team.manager, team.eventId, removed).andExpect { status { isNotFound() } }

            setEventStatus(team.eventId, EventStatus.OPEN)
            v1Buy(newUser("구매자"), team.master, team.eventId, keep, approval = false)
            deleteTicket(team.manager, team.eventId, keep).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Ticket_Item_400_7") }
            }
        }

        @Test
        fun `판매 중단이면 v1 장바구니 400, 재개하면 다시 구매 가능, 중단·재개는 멱등`() {
            val team = Team()
            val id = createTicket(team.manager, team.eventId, freeBody())
            setEventStatus(team.eventId, EventStatus.OPEN)
            val buyer = newUser("구매자")

            suspend(team.guest, team.eventId, id).andExpect { status { isForbidden() } }
            suspend(team.manager, team.eventId, id).andExpect {
                status { isOk() }
                jsonPath("$.data.saleState") { value("SUSPENDED") }
                jsonPath("$.data.isPurchasable") { value(false) }
            }
            suspend(team.manager, team.eventId, id).andExpect { status { isOk() }; jsonPath("$.data.saleState") { value("SUSPENDED") } }
            assertFalse(ticketItemRepository.findById(id).get().isSellable!!)
            v1Cart(buyer, id).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Ticket_Item_400_10") }
            }

            resume(team.manager, team.eventId, id).andExpect { status { isOk() }; jsonPath("$.data.saleState") { value("BEFORE_SALE") } }
            resume(team.manager, team.eventId, id).andExpect { status { isOk() }; jsonPath("$.data.isPurchasable") { value(true) } }
            v1Buy(buyer, team.master, team.eventId, id, approval = false)

            // 판매된 티켓 중단 → SUSPENDED 이지만 isSold 유지, 재개하면 SOLD
            suspend(team.manager, team.eventId, id).andExpect {
                jsonPath("$.data.saleState") { value("SUSPENDED") }
                jsonPath("$.data.isSold") { value(true) }
            }
            resume(team.manager, team.eventId, id).andExpect { jsonPath("$.data.saleState") { value("SOLD") } }
        }

        @Test
        fun `판매 시작 전이면 v1 장바구니 400`() {
            val team = Team()
            val id = createTicket(team.manager, team.eventId, freeBody(overrides = mapOf("saleStartAt" to LocalDateTime.now().plusDays(1).f())))
            setEventStatus(team.eventId, EventStatus.OPEN)
            assertFalse(manageItem(team.guest, team.eventId, id).at("/isPurchasable").asBoolean())
            v1Cart(newUser("구매자"), id).andExpect { jsonPath("$.code") { value("Ticket_Item_400_10") } }
        }
    }

    @Nested
    @DisplayName("v1 호환")
    inner class V1Compat {

        @Test
        fun `v2 로 만든 티켓은 v1 목록에 보이고 v1 주문 플로우로 구매되며 재고가 줄면 SOLD`() {
            val team = Team()
            val dudoong = createTicket(team.manager, team.eventId, dudoongBody(supplyCount = 5))
            // v1 무료 확정은 재고 감소 이벤트를 검증보다 먼저 발행해서, H2(read committed)에서는 남은 재고의 절반 초과 구매가 실패한다 (MySQL RR 은 통과). 그래서 여유 있게 잡는다
            val free = createTicket(team.manager, team.eventId, freeBody(supplyCount = 5))
            setEventStatus(team.eventId, EventStatus.OPEN)

            mockMvc.get("/api/v1/events/${team.eventId}/ticketItems").andExpect {
                status { isOk() }
                jsonPath("$.data.ticketItems.length()") { value(2) }
                jsonPath("$.data.ticketItems[0].ticketItemId") { value(dudoong) }
                jsonPath("$.data.ticketItems[0].payType") { value("두둥티켓") }
                jsonPath("$.data.ticketItems[0].approveType") { value("승인") }
                jsonPath("$.data.ticketItems[0].supplyCount") { value(5) }
                jsonPath("$.data.ticketItems[0].accountInfo.bankName") { value("신한은행") }
                jsonPath("$.data.ticketItems[1].payType") { value("무료티켓") }
                jsonPath("$.data.ticketItems[1].approveType") { value("선착순") }
            }

            val buyer = newUser("구매자")
            v1Buy(buyer, team.master, team.eventId, dudoong, approval = true)
            v1Buy(buyer, team.master, team.eventId, free, quantity = 2, approval = false)
            manage(team.guest, team.eventId).andExpect {
                jsonPath("$.data[0].saleState") { value("SOLD") }
                jsonPath("$.data[0].remaining") { value(4) }
                jsonPath("$.data[0].soldCount") { value(1) }
                jsonPath("$.data[1].saleState") { value("SOLD") }
                jsonPath("$.data[1].remaining") { value(3) }
            }
            assertTrue(ticketItemRepository.findById(free).get().isSold())
        }

        @Test
        fun `v1 로 만든 티켓도 v2 관리 목록에 보이고 v2 로 수정할 수 있다`() {
            val team = Team()
            val v1 = saveV1FreeTicket(team.eventId, supplyCount = 10)
            // v1 생성 API 도 그대로 동작
            mockMvc.post("/api/v1/events/${team.eventId}/ticketItems") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf(
                        "payType" to "무료티켓", "name" to "v1api", "description" to "v1", "price" to 0, "supplyCount" to 3,
                        "approveType" to "선착순", "isQuantityPublic" to true, "purchaseLimit" to 1,
                    ),
                )
            }.andExpect { status { isOk() } }
            manage(team.guest, team.eventId).andExpect {
                jsonPath("$.data.length()") { value(2) }
                jsonPath("$.data[0].ticketItemId") { value(v1.id!!) }
                jsonPath("$.data[0].payType") { value("FREE") }
                jsonPath("$.data[0].supplyCount") { value(10) }
                jsonPath("$.data[0].purchaseLimit") { value(2) }
                jsonPath("$.data[0].saleState") { value("BEFORE_SALE") }
                jsonPath("$.data[0].saleStartAt") { value(null as Any?) }
                jsonPath("$.data[1].name") { value("v1api") }
            }
            patchTicket(team.manager, team.eventId, v1.id!!, freeBody(name = "v2수정", supplyCount = 12)).andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("v2수정") }
                jsonPath("$.data.remaining") { value(12) }
            }
        }
    }
}
