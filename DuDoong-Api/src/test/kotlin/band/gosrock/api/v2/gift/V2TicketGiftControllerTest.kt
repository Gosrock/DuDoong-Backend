package band.gosrock.api.v2.gift

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.gift.domain.TicketGiftCancelReason
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.service.UserDomainService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

/** v2 티켓탭·선물 통합 테스트 (#719): 전이표 전체 행, 경로별 차단 표(v1·v2·운영), 에러 코드, viewState 판정 순서, 연쇄 취소, uuid 교체, 알림 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 사용자 앱 - 티켓탭·선물")
class V2TicketGiftControllerTest : V2GiftTestSupport() {

    @Autowired private lateinit var userDomainService: UserDomainService

    @Nested
    @DisplayName("G-1 선물 링크 생성")
    inner class Create {

        @Test
        fun `생성 - PENDING, 토큰 base64url 43자, linkPath, 메모 앞뒤 공백 제거, 티켓 소유자·uuid 그대로`() {
            val shop = Shop()
            val sender = newBuyer("보낸이")
            val (orderUuid, uuids) = approvedOrder(shop, sender)
            val data = giftOk(sender, uuids[0], "  동생  ")
            val token = data.at("/giftToken").asText()
            assertEquals(43, token.length)
            assertTrue(token.matches(Regex("[A-Za-z0-9_-]+")))
            assertEquals("/gifts/$token", data.at("/linkPath").asText())
            assertEquals("동생", data.at("/memo").asText())
            val gift = giftOf(data.at("/giftId").asLong())
            assertEquals(TicketGiftStatus.PENDING, gift.status)
            assertEquals(sender.id, gift.senderUserId)
            assertEquals(orderUuid, gift.orderUuid)
            assertEquals(shop.eventId, gift.eventId)
            assertNull(gift.receiverUserId)
            val ticket = ticketByUuid(uuids[0])
            assertEquals(sender.id, ticket.getUserId())
            assertEquals(gift.issuedTicketId, ticket.id)
        }

        @Test
        fun `메모 - 없음·빈 값은 null, 50자 성공, 51자 Gift_400_9 (선물 미생성)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            assertTrue(giftOk(sender, uuids[0], "   ").at("/memo").isNull)
            assertEquals("Gift_400_9", gift(sender, uuids[1], "가".repeat(51)).andExpect { status { isBadRequest() } }.code())
            assertTrue(ticketGiftRepository.findAllByIssuedTicketIdIn(listOf(ticketByUuid(uuids[1]).id!!)).isEmpty())
            assertEquals("가".repeat(50), giftOk(sender, uuids[1], "가".repeat(50)).at("/memo").asText())
        }

        @Test
        fun `남의 티켓·없는 티켓은 IssuedTicket_404_1, 비로그인은 401`() {
            val shop = Shop()
            val (_, uuids) = approvedOrder(shop, newBuyer())
            assertEquals("IssuedTicket_404_1", gift(newBuyer(), uuids[0]).andExpect { status { isNotFound() } }.code())
            assertEquals("IssuedTicket_404_1", gift(newBuyer(), "no-such-uuid").andExpect { status { isNotFound() } }.code())
            gift(null, uuids[0]).andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `이미 대기 중이면 Gift_400_2`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            giftOk(sender, uuids[0])
            assertEquals("Gift_400_2", gift(sender, uuids[0]).andExpect { status { isBadRequest() } }.code())
            assertEquals(1, ticketGiftRepository.findAllByIssuedTicketIdIn(listOf(ticketByUuid(uuids[0]).id!!)).size)
        }

        @Test
        fun `선물할 수 없는 티켓은 Gift_400_1 - 입장함, 공연 시작 후, 공연 OPEN 아님, 취소된 주문`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            checkIn(shop.team.guest, shop.eventId, uuids[0]).andExpect { status { isOk() } }
            assertEquals("Gift_400_1", gift(sender, uuids[0]).andExpect { status { isBadRequest() } }.code())

            setEventStatus(shop.eventId, EventStatus.PREPARING)
            assertEquals("Gift_400_1", gift(sender, uuids[1]).andExpect { status { isBadRequest() } }.code())
            setEventStatus(shop.eventId, EventStatus.OPEN)
            startEvent(shop.eventId)
            assertEquals("Gift_400_1", gift(sender, uuids[1]).andExpect { status { isBadRequest() } }.code())

            val shop2 = Shop()
            val buyer2 = newBuyer()
            val (order2, uuids2) = approvedOrder(shop2, buyer2)
            v2HostCancel(shop2.team.manager, shop2.eventId, order2).andExpect { status { isOk() } }
            assertEquals("Gift_400_1", gift(buyer2, uuids2[0]).andExpect { status { isBadRequest() } }.code())
            assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
        }

        @Test
        fun `정지된 보낸 사람·삭제된 공연은 Gift_400_1 (락 안에서 계정·공연 상태 확인)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            userRepository.save(userRepository.findById(sender.id!!).get().also { it.changeAccountState(AccountState.SUSPENDED) })
            assertEquals("Gift_400_1", gift(sender, uuids[0]).andExpect { status { isBadRequest() } }.code())
            userRepository.save(userRepository.findById(sender.id!!).get().also { it.changeAccountState(AccountState.NORMAL) })
            setEventStatus(shop.eventId, EventStatus.DELETED)
            assertEquals("Gift_400_1", gift(sender, uuids[0]).andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `무료 즉시 발급 티켓도 선물 가능`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false)
            val sender = newBuyer()
            val orderUuid = v2OrderOk(sender, freeBodyOf(shop, free)).at("/orderUuid").asText()
            giftOk(sender, shop.ticketUuids(orderUuid)[0])
        }

        @Test
        fun `받은 티켓은 다시 선물할 수 없다(Gift_400_1), 반환·거절·회수로 돌아온 티켓은 다시 선물 가능`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val (_, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            assertEquals("Gift_400_1", gift(receiver, newUuid).andExpect { status { isBadRequest() } }.code())

            // 반환 → 재선물
            returnTicket(receiver, newUuid).andExpect { status { isOk() } }
            val back = ticketOf(giftOf(ticketGiftRepository.findAll().last { it.receiverUserId == receiver.id }.id!!).issuedTicketId).uuid!!
            val regift = giftOk(sender, back)
            // 거절 → 재선물
            reject(newBuyer(), regift.at("/giftToken").asText()).andExpect { status { isOk() } }
            val regift2 = giftOk(sender, back)
            // 회수 → 재선물
            cancelGift(sender, regift2.at("/giftId").asLong()).andExpect { status { isOk() } }
            giftOk(sender, back)
        }
    }

    @Nested
    @DisplayName("전이표 (PENDING → CANCELED / REJECTED / ACCEPTED → RETURNED)")
    inner class Transitions {

        @Test
        fun `G-4 수락 - ACCEPTED, 소유자 정보 교체, uuid 교체, 주문자 그대로, 옛 uuid 는 T-2·v1 상세 404`() {
            val shop = Shop()
            val sender = newBuyer("보낸이", "010-1111-1111")
            val receiver = newBuyer("받은이", "010-2222-2222")
            val (orderUuid, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0])
            val result = accept(receiver, created.at("/giftToken").asText()).andExpect { status { isOk() } }.data()
            assertEquals("ACCEPTED", result.at("/status").asText())
            val newUuid = result.at("/ticketUuid").asText()
            assertNotEquals(uuids[0], newUuid)

            val ticket = ticketByUuid(newUuid)
            assertEquals(receiver.id, ticket.userInfo!!.userId)
            assertEquals("받은이", ticket.userInfo!!.userName)
            assertEquals(receiver.profile!!.email, ticket.userInfo!!.email)
            assertEquals(sender.id, orderRepository.findByOrderUuid(orderUuid).get().userId)
            val gift = giftOf(created.at("/giftId").asLong())
            assertEquals(TicketGiftStatus.ACCEPTED, gift.status)
            assertEquals(receiver.id, gift.receiverUserId)
            assertTrue(gift.acceptedAt != null)

            assertTrue(issuedTicketRepository.findByUuid(uuids[0]).isEmpty)
            assertEquals("IssuedTicket_404_1", myTicket(sender, uuids[0]).andExpect { status { isNotFound() } }.code())
            assertEquals("IssuedTicket_404_1", v1TicketDetail(sender, uuids[0]).andExpect { status { isNotFound() } }.code())
            // 받은 사람은 새 uuid 로 QR 이 있다
            val detail = myTicket(receiver, newUuid).andExpect { status { isOk() } }.data()
            assertEquals(newUuid, detail.at("/qrValue").asText())
            assertEquals("RECEIVED", detail.at("/giftState").asText())
            assertTrue(detail.at("/orderUuid").isNull)
            assertEquals("보낸이", detail.at("/gift/senderName").asText())
            assertTrue(detail.at("/canReturn").asBoolean())
            assertFalse(detail.at("/canGift").asBoolean())
        }

        @Test
        fun `G-4 수락 뒤 옛 QR - v2 호스트 스캔 OTHER_EVENT, v1 입장 IssuedTicket_404_1, 새 QR 은 입장 가능`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val (_, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            assertEquals("OTHER_EVENT", checkIn(shop.team.guest, shop.eventId, uuids[0]).andExpect { status { isOk() } }.data().at("/result").asText())
            assertEquals("IssuedTicket_404_1", v1Entrance(shop.team.manager, shop.eventId, uuids[0]).andExpect { status { isNotFound() } }.code())
            v1Entrance(shop.team.manager, shop.eventId, newUuid).andExpect { status { isOk() } }
            assertEquals(IssuedTicketStatus.ENTRANCE_COMPLETED, ticketByUuid(newUuid).issuedTicketStatus)
        }

        @Test
        fun `G-4 수락은 공연 시작 후 종료 전까지 가능`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val token = giftOk(sender, uuids[0]).at("/giftToken").asText()
            startEvent(shop.eventId)
            accept(newBuyer(), token).andExpect { status { isOk() } }
        }

        @Test
        fun `G-4 수락 오류 - 판정 순서 - 대기 아님(400_3) → 만료(400_5) → 본인 링크(400_4) → 원 주문 비정상(400_6), 없는 토큰 404, 비로그인 401`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            val token = giftOk(sender, uuids[0]).at("/giftToken").asText()
            assertEquals("Gift_404_1", accept(newBuyer(), "no-such-token").andExpect { status { isNotFound() } }.code())
            accept(null, token).andExpect { status { isUnauthorized() } }
            assertEquals("Gift_400_4", accept(sender, token).andExpect { status { isBadRequest() } }.code())

            // 원 주문 비정상 (방어: 연쇄 처리를 거치지 않고 주문 상태만 바꿈)
            val order = orderRepository.findByOrderUuid(orderUuid).get()
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CANCELED)
            orderRepository.save(order)
            assertEquals("Gift_400_6", accept(newBuyer(), token).andExpect { status { isBadRequest() } }.code())
            assertEquals("Gift_400_4", accept(sender, token).andExpect { status { isBadRequest() } }.code())
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.APPROVED)
            orderRepository.save(order)

            // 만료가 본인 링크보다 먼저
            endEvent(shop.eventId)
            assertEquals("Gift_400_5", accept(newBuyer(), token).andExpect { status { isBadRequest() } }.code())
            assertEquals("Gift_400_5", accept(sender, token).andExpect { status { isBadRequest() } }.code())
            assertEquals("Gift_400_5", reject(newBuyer(), token).andExpect { status { isBadRequest() } }.code())

            // 대기 아님이 만료보다 먼저
            val giftId = ticketGiftRepository.findByToken(token)!!.id!!
            cancelGift(sender, giftId).andExpect { status { isOk() } }
            assertEquals("Gift_400_3", accept(newBuyer(), token).andExpect { status { isBadRequest() } }.code())
            assertEquals("Gift_400_3", accept(sender, token).andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `선물 만료 - 공연 상태가 정산중·지난공연이면 시각과 무관하게 만료 (Gift_400_5)`() {
            for (status in listOf(EventStatus.CALCULATING, EventStatus.CLOSED)) {
                val shop = Shop()
                val sender = newBuyer()
                val (_, uuids) = approvedOrder(shop, sender)
                val token = giftOk(sender, uuids[0]).at("/giftToken").asText()
                setEventStatus(shop.eventId, status)
                assertEquals("Gift_400_5", accept(newBuyer(), token).andExpect { status { isBadRequest() } }.code(), status.name)
                assertEquals("EXPIRED", landing(newBuyer(), token).at("/viewState").asText())
            }
        }

        @Test
        fun `Gift_400_5 문구는 종료·준비중 공연 모두 '선물을 받을 수 없는 공연입니다' (#752)`() {
            val ended = Shop()
            val endedSender = newBuyer()
            val endedToken = giftOk(endedSender, approvedOrder(ended, endedSender).second[0]).at("/giftToken").asText()
            endEvent(ended.eventId)
            val preparing = Shop()
            val preparingSender = newBuyer()
            val preparingToken = giftOk(preparingSender, approvedOrder(preparing, preparingSender).second[0]).at("/giftToken").asText()
            // 연쇄 처리 없이 상태만 바꿔 대기 선물을 남긴다 (정상 흐름에서는 준비중 전환이 대기 선물을 취소)
            setEventStatus(preparing.eventId, EventStatus.PREPARING)
            for (token in listOf(endedToken, preparingToken)) {
                accept(newBuyer(), token).andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("Gift_400_5") }
                    jsonPath("$.reason") { value("선물을 받을 수 없는 공연입니다.") }
                }
            }
        }

        @Test
        fun `G-4 이미 수락된 링크는 Gift_400_3 (1회용)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val token = giftOk(sender, uuids[0]).at("/giftToken").asText()
            accept(newBuyer(), token).andExpect { status { isOk() } }
            assertEquals("Gift_400_3", accept(newBuyer(), token).andExpect { status { isBadRequest() } }.code())
            assertEquals("Gift_400_3", reject(newBuyer(), token).andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `G-5 거절 - REJECTED, 거절한 사람 기록, 티켓은 보낸 사람 그대로(uuid 유지), QR 다시 활성`() {
            val shop = Shop()
            val sender = newBuyer()
            val rejecter = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0])
            assertTrue(myTicket(sender, uuids[0]).andExpect { status { isOk() } }.data().at("/qrValue").isNull)
            assertEquals("Gift_400_4", reject(sender, created.at("/giftToken").asText()).andExpect { status { isBadRequest() } }.code())
            assertEquals("REJECTED", reject(rejecter, created.at("/giftToken").asText()).andExpect { status { isOk() } }.data().at("/status").asText())
            val gift = giftOf(created.at("/giftId").asLong())
            assertEquals(TicketGiftStatus.REJECTED, gift.status)
            assertEquals(rejecter.id, gift.receiverUserId)
            val ticket = ticketByUuid(uuids[0])
            assertEquals(sender.id, ticket.getUserId())
            assertEquals(uuids[0], myTicket(sender, uuids[0]).andExpect { status { isOk() } }.data().at("/qrValue").asText())
        }

        @Test
        fun `G-2 회수 - CANCELED(SENDER), 대기 아니면 Gift_400_3, 남의 선물·없는 선물 Gift_404_1, 공연 종료 후에도 가능`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val g1 = giftOk(sender, uuids[0]).at("/giftId").asLong()
            assertEquals("Gift_404_1", cancelGift(newBuyer(), g1).andExpect { status { isNotFound() } }.code())
            assertEquals("Gift_404_1", cancelGift(sender, 999_999_999L).andExpect { status { isNotFound() } }.code())
            val result = cancelGift(sender, g1).andExpect { status { isOk() } }.data()
            assertEquals("CANCELED", result.at("/status").asText())
            assertEquals("SENDER", result.at("/cancelReason").asText())
            assertEquals("Gift_400_3", cancelGift(sender, g1).andExpect { status { isBadRequest() } }.code())
            assertEquals(TicketGiftCancelReason.SENDER, giftOf(g1).cancelReason)

            val g2 = giftOk(sender, uuids[1]).at("/giftId").asLong()
            endEvent(shop.eventId)
            cancelGift(sender, g2).andExpect { status { isOk() } }
            assertEquals(TicketGiftStatus.CANCELED, giftOf(g2).status)
        }

        @Test
        fun `G-2 수락된 선물은 회수할 수 없다 (Gift_400_3)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val (giftId, _) = giveAndAccept(sender, newBuyer(), uuids[0])
            assertEquals("Gift_400_3", cancelGift(sender, giftId).andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `G-8 메모 수정 - 대기 중만, 빈 값은 삭제, 남의 선물 404, 대기 아님 400_3, 51자 400_9`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val giftId = giftOk(sender, uuids[0], "처음").at("/giftId").asLong()
            assertEquals("친구", changeMemo(sender, giftId, " 친구 ").andExpect { status { isOk() } }.data().at("/memo").asText())
            assertEquals("친구", giftOf(giftId).memo)
            assertEquals("Gift_400_9", changeMemo(sender, giftId, "가".repeat(51)).andExpect { status { isBadRequest() } }.code())
            assertEquals("Gift_404_1", changeMemo(newBuyer(), giftId, "남").andExpect { status { isNotFound() } }.code())
            changeMemo(sender, giftId, "").andExpect { status { isOk() } }
            assertNull(giftOf(giftId).memo)
            cancelGift(sender, giftId).andExpect { status { isOk() } }
            assertEquals("Gift_400_3", changeMemo(sender, giftId, "늦음").andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `G-6 반환 - RETURNED, 보낸 사람에게 소유자 복귀, uuid 다시 교체(받은 사람 uuid 무효)`() {
            val shop = Shop()
            val sender = newBuyer("보낸이")
            val receiver = newBuyer("받은이")
            val (_, uuids) = approvedOrder(shop, sender)
            val (giftId, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            assertEquals("RETURNED", returnTicket(receiver, newUuid).andExpect { status { isOk() } }.data().at("/status").asText())
            val gift = giftOf(giftId)
            assertEquals(TicketGiftStatus.RETURNED, gift.status)
            assertTrue(gift.returnedAt != null)
            val ticket = ticketOf(gift.issuedTicketId)
            assertEquals(sender.id, ticket.getUserId())
            assertEquals("보낸이", ticket.userInfo!!.userName)
            assertNotEquals(newUuid, ticket.uuid)
            assertNotEquals(uuids[0], ticket.uuid)
            assertEquals("IssuedTicket_404_1", myTicket(receiver, newUuid).andExpect { status { isNotFound() } }.code())
            assertEquals("NONE", myTicket(sender, ticket.uuid!!).andExpect { status { isOk() } }.data().at("/giftState").asText())
        }

        @Test
        fun `G-6 반환 오류 - 받은 티켓 아님(400_8), 남의 티켓 404, 입장함·공연 시작 후·보낸 사람 정지(400_7)`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            assertEquals("Gift_400_8", returnTicket(sender, uuids[1]).andExpect { status { isBadRequest() } }.code())
            assertEquals("IssuedTicket_404_1", returnTicket(receiver, uuids[1]).andExpect { status { isNotFound() } }.code())
            val (_, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            assertEquals("IssuedTicket_404_1", returnTicket(sender, newUuid).andExpect { status { isNotFound() } }.code())

            // 보낸 사람 정지
            userRepository.save(userRepository.findById(sender.id!!).get().also { it.changeAccountState(AccountState.SUSPENDED) })
            assertEquals("Gift_400_7", returnTicket(receiver, newUuid).andExpect { status { isBadRequest() } }.code())
            userRepository.save(userRepository.findById(sender.id!!).get().also { it.changeAccountState(AccountState.NORMAL) })

            startEvent(shop.eventId)
            assertEquals("Gift_400_7", returnTicket(receiver, newUuid).andExpect { status { isBadRequest() } }.code())
            assertFalse(myTicket(receiver, newUuid).andExpect { status { isOk() } }.data().at("/canReturn").asBoolean())
        }

        @Test
        fun `G-6 입장한 받은 티켓은 반환 불가 (Gift_400_7)`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val (_, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            checkIn(shop.team.guest, shop.eventId, newUuid).andExpect { status { isOk() } }
            assertEquals("Gift_400_7", returnTicket(receiver, newUuid).andExpect { status { isBadRequest() } }.code())
        }
    }

    @Nested
    @DisplayName("G-3 선물 랜딩 viewState")
    inner class Landing {

        @Test
        fun `대기 중 - 비로그인 AVAILABLE(로그인 유도), 로그인 AVAILABLE, 보낸 사람 OWN_LINK(+giftId), 공연·티켓·보낸 사람 요약 포함`() {
            val shop = Shop()
            val sender = newBuyer("보낸닉")
            val (_, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0], "비밀메모")
            val token = created.at("/giftToken").asText()
            val anonymous = landing(null, token)
            assertEquals("AVAILABLE", anonymous.at("/viewState").asText())
            assertFalse(anonymous.at("/isLoggedIn").asBoolean())
            assertTrue(anonymous.at("/giftId").isNull)
            assertEquals("보*닉", anonymous.at("/senderName").asText(), "공개 랜딩은 이름 가운데를 가린다")
            assertEquals(shop.eventId, anonymous.at("/event/eventId").asLong())
            assertEquals("일반", anonymous.at("/ticket/ticketName").asText())
            assertEquals(6000, anonymous.at("/ticket/unitPrice").asLong())
            assertTrue(anonymous.at("/ticket/optionAnswers").isMissingNode, "옵션 답변(개인 입력)은 주지 않는다")
            assertFalse(anonymous.toString().contains("홍길동"))
            assertFalse(anonymous.toString().contains("비밀메모"))
            assertEquals("AVAILABLE", landing(newBuyer(), token).at("/viewState").asText())
            val own = landing(sender, token)
            assertEquals("OWN_LINK", own.at("/viewState").asText())
            assertEquals(created.at("/giftId").asLong(), own.at("/giftId").asLong())
        }

        @Test
        fun `없는 토큰은 Gift_404_1`() {
            assertEquals("Gift_404_1", v2Get(null, "/gifts/nope").andExpect { status { isNotFound() } }.code())
        }

        @Test
        fun `판정 순서 - 만료가 본인 링크보다 먼저 (보낸 사람도 EXPIRED)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val token = giftOk(sender, uuids[0]).at("/giftToken").asText()
            endEvent(shop.eventId)
            assertEquals("EXPIRED", landing(sender, token).at("/viewState").asText())
            assertEquals("EXPIRED", landing(null, token).at("/viewState").asText())
            assertEquals("PENDING", landing(null, token).at("/status").asText())
        }

        @Test
        fun `판정 순서 - 선물 상태가 만료보다 먼저, 대기 중이 아니면 상태만 (요약 없음)`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val accepted = giftOk(sender, uuids[0]).at("/giftToken").asText()
            accept(receiver, accepted).andExpect { status { isOk() } }
            val canceled = giftOk(sender, uuids[1])
            cancelGift(sender, canceled.at("/giftId").asLong()).andExpect { status { isOk() } }
            endEvent(shop.eventId)

            val mine = landing(receiver, accepted)
            assertEquals("ALREADY_ACCEPTED", mine.at("/viewState").asText())
            assertTrue(mine.at("/isReceiver").asBoolean())
            assertTrue(mine.at("/event").isNull)
            assertTrue(mine.at("/ticket").isNull)
            assertTrue(mine.at("/senderName").isNull)
            assertFalse(landing(newBuyer(), accepted).at("/isReceiver").asBoolean())
            assertFalse(landing(null, accepted).at("/isReceiver").asBoolean())
            assertEquals("ALREADY_ACCEPTED", landing(sender, accepted).at("/viewState").asText())
            assertEquals("CANCELED", landing(sender, canceled.at("/giftToken").asText()).at("/viewState").asText())
        }

        @Test
        fun `거절됨 REJECTED, 반환됨 RETURNED`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val rejected = giftOk(sender, uuids[0]).at("/giftToken").asText()
            reject(receiver, rejected).andExpect { status { isOk() } }
            assertEquals("REJECTED", landing(null, rejected).at("/viewState").asText())
            val returned = giftOk(sender, uuids[1]).at("/giftToken").asText()
            val newUuid = accept(receiver, returned).andExpect { status { isOk() } }.data().at("/ticketUuid").asText()
            returnTicket(receiver, newUuid).andExpect { status { isOk() } }
            assertEquals("RETURNED", landing(receiver, returned).at("/viewState").asText())
        }
    }

    @Nested
    @DisplayName("경로별 차단 표 (v1·v2·운영)")
    inner class Blocking {

        @Test
        fun `v2 호스트 스캔 - 선물 대기 티켓은 GIFT_PENDING(입장 안 함), 회수하면 입장`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val giftId = giftOk(sender, uuids[0]).at("/giftId").asLong()
            assertEquals("GIFT_PENDING", checkIn(shop.team.guest, shop.eventId, uuids[0]).andExpect { status { isOk() } }.data().at("/result").asText())
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, ticketByUuid(uuids[0]).issuedTicketStatus)
            cancelGift(sender, giftId).andExpect { status { isOk() } }
            assertEquals("ENTERED", checkIn(shop.team.guest, shop.eventId, uuids[0]).andExpect { status { isOk() } }.data().at("/result").asText())
        }

        @Test
        fun `v2 셀프 체크인 - 선물 대기 티켓은 후보에서 제외, 직접 지정하면 GIFT_PENDING, 대기 티켓만 있으면 GIFT_PENDING`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            giftOk(sender, uuids[0])
            val token = qrToken(shop.team.guest, shop.eventId)
            assertEquals("GIFT_PENDING", selfCheckIn(sender, token, uuids[0]).andExpect { status { isOk() } }.data().at("/result").asText())
            // 대기 아닌 1장만 후보 → 바로 입장
            val auto = selfCheckIn(sender, token).andExpect { status { isOk() } }.data()
            assertEquals("ENTERED", auto.at("/result").asText())
            assertEquals(uuids[1], auto.at("/ticket/ticketUuid").asText())
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, ticketByUuid(uuids[0]).issuedTicketStatus)

            val shop2 = Shop()
            val only = newBuyer()
            val (_, single) = approvedOrder(shop2, only)
            giftOk(only, single[0])
            assertEquals("GIFT_PENDING", selfCheckIn(only, qrToken(shop2.team.guest, shop2.eventId)).andExpect { status { isOk() } }.data().at("/result").asText())
        }

        @Test
        fun `v2 셀프 체크인 - 입장 전 3장 중 1장 대기면 후보 2장 (SELECT_TICKET)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, a) = approvedOrder(shop, sender, quantity = 2)
            val (_, b) = approvedOrder(shop, sender, quantity = 1)
            giftOk(sender, a[0])
            val data = selfCheckIn(sender, qrToken(shop.team.guest, shop.eventId)).andExpect { status { isOk() } }.data()
            assertEquals("SELECT_TICKET", data.at("/result").asText())
            assertEquals(setOf(a[1], b[0]), data.at("/candidates").map { it.at("/ticketUuid").asText() }.toSet())
        }

        @Test
        fun `v1 입장 - 선물 대기 IssuedTicket_400_8 (입장 안 함)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            giftOk(sender, uuids[0])
            assertEquals("IssuedTicket_400_8", v1Entrance(shop.team.manager, shop.eventId, uuids[0]).andExpect { status { isBadRequest() } }.code())
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, ticketByUuid(uuids[0]).issuedTicketStatus)
        }

        @Test
        fun `v1 티켓 상세 - 선물 대기 IssuedTicket_400_8, 선물 완료(옛 uuid) 404`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            giftOk(sender, uuids[0])
            assertEquals("IssuedTicket_400_8", v1TicketDetail(sender, uuids[0]).andExpect { status { isBadRequest() } }.code())
            giveAndAccept(sender, newBuyer(), uuids[1])
            assertEquals("IssuedTicket_404_1", v1TicketDetail(sender, uuids[1]).andExpect { status { isNotFound() } }.code())
        }

        @Test
        fun `v1 주문 티켓 목록 - 선물 대기는 uuid(QR) 비움, 선물 완료는 빠짐, 선물 없는 주문은 그대로`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            val before = v1OrderTickets(sender, orderUuid)
            assertEquals(uuids, before.map { it.at("/uuid").asText() })

            giftOk(sender, uuids[0])
            val pending = v1OrderTickets(sender, orderUuid)
            assertEquals(2, pending.size())
            assertTrue(pending[0].at("/uuid").isNull)
            assertEquals(uuids[1], pending[1].at("/uuid").asText())

            cancelGift(sender, ticketGiftRepository.findAll().last { it.senderUserId == sender.id }.id!!).andExpect { status { isOk() } }
            giveAndAccept(sender, newBuyer(), uuids[0])
            val sent = v1OrderTickets(sender, orderUuid)
            assertEquals(listOf(uuids[1]), sent.map { it.at("/uuid").asText() })
        }

        @Test
        fun `v2 O-4 취소 - 선물 대기·선물 완료 티켓이 있으면 Order_400_24, canCancel false, 회수하면 취소 가능`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            val giftId = giftOk(sender, uuids[0]).at("/giftId").asLong()
            assertFalse(myOrder(sender, orderUuid).andExpect { status { isOk() } }.data().at("/canCancel").asBoolean())
            assertEquals("Order_400_24", cancelMy(sender, orderUuid, refundAccount).andExpect { status { isBadRequest() } }.code())
            cancelGift(sender, giftId).andExpect { status { isOk() } }
            assertTrue(myOrder(sender, orderUuid).andExpect { status { isOk() } }.data().at("/canCancel").asBoolean())

            giveAndAccept(sender, newBuyer(), uuids[1])
            assertEquals("Order_400_24", cancelMy(sender, orderUuid, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
        }

        @Test
        fun `v1 사용자 환불 - 선물 대기·선물 완료 티켓이 있으면 Order_400_24, 선물 없는 주문은 기존대로 환불`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            val giftId = giftOk(sender, uuids[0]).at("/giftId").asLong()
            assertEquals("Order_400_24", v1Refund(sender, orderUuid).andExpect { status { isBadRequest() } }.code())
            cancelGift(sender, giftId).andExpect { status { isOk() } }
            giveAndAccept(sender, newBuyer(), uuids[0])
            assertEquals("Order_400_24", v1Refund(sender, orderUuid).andExpect { status { isBadRequest() } }.code())
            assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)

            val plain = approvedOrder(shop, newBuyer()).first
            v1Refund(orderRepository.findByOrderUuid(plain).get().userId!!.let { userRepository.findById(it).get() }, plain).andExpect { status { isOk() } }
            assertEquals(OrderStatus.REFUND, orderRepository.findByOrderUuid(plain).get().orderStatus)
        }

        @Test
        fun `T-2 - 선물 대기 티켓은 qrValue 없음 + 링크·메모, 취소 티켓도 qrValue 없음`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0], "메모")
            val detail = myTicket(sender, uuids[0]).andExpect { status { isOk() } }.data()
            assertTrue(detail.at("/qrValue").isNull)
            assertEquals("PENDING", detail.at("/giftState").asText())
            assertEquals("GIFT_PENDING", detail.at("/state").asText())
            assertEquals(created.at("/giftToken").asText(), detail.at("/gift/giftToken").asText())
            assertEquals("메모", detail.at("/gift/memo").asText())
            assertEquals(orderUuid, detail.at("/orderUuid").asText())
            assertFalse(detail.at("/canGift").asBoolean())

            cancelGift(sender, created.at("/giftId").asLong()).andExpect { status { isOk() } }
            assertTrue(myTicket(sender, uuids[0]).andExpect { status { isOk() } }.data().at("/canGift").asBoolean())
            v2HostCancel(shop.team.manager, shop.eventId, orderUuid).andExpect { status { isOk() } }
            val canceled = myTicket(sender, uuids[0]).andExpect { status { isOk() } }.data()
            assertTrue(canceled.at("/qrValue").isNull)
            assertEquals("CANCELED", canceled.at("/state").asText())
        }
    }

    @Nested
    @DisplayName("연쇄 처리 (DEC-026 #8·#9·#10)")
    inner class Cascade {

        /** 한 주문 2장: [0] 선물 대기, [1] 선물 완료 → (orderUuid, 대기 giftId, 완료 giftId, 받은 사람) */
        private fun pendingAndAccepted(shop: Shop, sender: band.gosrock.domain.domains.user.domain.User): Quad {
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            val pending = giftOk(sender, uuids[0]).at("/giftId").asLong()
            val receiver = newBuyer("받은이")
            val (accepted, newUuid) = giveAndAccept(sender, receiver, uuids[1])
            return Quad(orderUuid, pending, accepted, receiver, newUuid)
        }

        private fun assertOrderCascade(q: Quad) {
            val pending = giftOf(q.pendingGiftId)
            assertEquals(TicketGiftStatus.CANCELED, pending.status)
            assertEquals(TicketGiftCancelReason.ORDER_CANCELED, pending.cancelReason)
            val accepted = giftOf(q.acceptedGiftId)
            assertEquals(TicketGiftStatus.ACCEPTED, accepted.status)
            val receivedTicket = ticketOf(accepted.issuedTicketId)
            assertEquals(IssuedTicketStatus.CANCELED, receivedTicket.issuedTicketStatus)
            assertEquals(q.receiver.id, receivedTicket.getUserId())
            assertEquals(IssuedTicketStatus.CANCELED, ticketOf(pending.issuedTicketId).issuedTicketStatus)
            assertEquals(1, awaitNotification(q.receiver, NotificationType.GIFT_TICKET_CANCELED))
            assertEquals("CANCELED", myTicket(q.receiver, q.receiverUuid).andExpect { status { isOk() } }.data().at("/state").asText())
        }

        @Test
        fun `v2 호스트 취소(R-5) - 대기 선물 CANCELED(ORDER_CANCELED), 선물 완료 티켓 취소(기록은 ACCEPTED) + 받은 사람 알림`() {
            val shop = Shop()
            val q = pendingAndAccepted(shop, newBuyer())
            v2HostCancel(shop.team.manager, shop.eventId, q.orderUuid).andExpect { status { isOk() } }
            assertOrderCascade(q)
        }

        @Test
        fun `v1 호스트 취소 - v2 와 같은 연쇄`() {
            val shop = Shop()
            val q = pendingAndAccepted(shop, newBuyer())
            v1HostCancel(shop.team.manager, shop.eventId, q.orderUuid).andExpect { status { isOk() } }
            assertOrderCascade(q)
        }

        @Test
        fun `운영 어드민 취소 - 같은 연쇄 (주문 락 경로로 처리)`() {
            val shop = Shop()
            val q = pendingAndAccepted(shop, newBuyer())
            adminCancel(newAdmin(), q.orderUuid).andExpect { status { is2xxSuccessful() } }
            assertEquals(OrderStatus.CANCELED, orderRepository.findByOrderUuid(q.orderUuid).get().orderStatus)
            assertOrderCascade(q)
        }

        @Test
        fun `운영 취소 뒤 운영 환불 완료·환불 상태 변경 - 선물 상태 그대로, 받은 사람 취소 알림 1건, 주문자 알림(#726)과 겹치지 않음`() {
            val shop = Shop()
            val sender = newBuyer()
            val q = pendingAndAccepted(shop, sender)
            val admin = newAdmin()
            adminCancel(admin, q.orderUuid).andExpect { status { is2xxSuccessful() } }
            mockMvc.patch("/internal-api/v1/refunds/${q.orderUuid}/complete") { with(user(admin.id.toString()).roles("ADMIN")) }
                .andExpect { status { is2xxSuccessful() } }
            mockMvc.patch("/internal-api/v1/orders/${q.orderUuid}/refund-status") {
                with(user(admin.id.toString()).roles("ADMIN"))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("refundStatus" to "REFUND_COMPLETED"))
            }.andExpect { status { is2xxSuccessful() } }
            assertEquals(TicketGiftStatus.CANCELED, giftOf(q.pendingGiftId).status)
            assertEquals(TicketGiftStatus.ACCEPTED, giftOf(q.acceptedGiftId).status)
            assertEquals(1, awaitNotification(q.receiver, NotificationType.GIFT_TICKET_CANCELED))
            assertEquals(1, awaitNotification(sender, NotificationType.ORDER_CANCELED_BY_HOST))
            assertEquals(0, notificationCount(q.receiver, NotificationType.ORDER_CANCELED_BY_HOST))
            assertEquals(0, notificationCount(sender, NotificationType.GIFT_TICKET_CANCELED))
        }

        @Test
        fun `호스트 취소 뒤 환불 완료(F-2) - 선물 상태 그대로, 오류 없음`() {
            val shop = Shop()
            val q = pendingAndAccepted(shop, newBuyer())
            v2HostCancel(shop.team.manager, shop.eventId, q.orderUuid).andExpect { status { isOk() } }
            v2Post(shop.team.manager, "/events/${shop.eventId}/refunds/${q.orderUuid}/complete").andExpect { status { isOk() } }
            assertEquals(TicketGiftStatus.CANCELED, giftOf(q.pendingGiftId).status)
            assertEquals(TicketGiftStatus.ACCEPTED, giftOf(q.acceptedGiftId).status)
        }

        @Test
        fun `보낸 사람 탈퇴 - 보낸 대기 선물 CANCELED(SENDER_WITHDRAWN), 수락된 선물·받은 티켓은 그대로`() {
            val shop = Shop()
            val sender = newBuyer()
            val q = pendingAndAccepted(shop, sender)
            userDomainService.withDrawUser(sender.id!!)
            assertEquals(TicketGiftCancelReason.SENDER_WITHDRAWN, giftOf(q.pendingGiftId).cancelReason)
            assertEquals(TicketGiftStatus.ACCEPTED, giftOf(q.acceptedGiftId).status)
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, ticketByUuid(q.receiverUuid).issuedTicketStatus)
        }

        @Test
        fun `운영 사용자 정지 - 보낸 대기 선물 CANCELED(SENDER_WITHDRAWN), 다시 NORMAL 로 바꾸는 것은 영향 없음`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val g1 = giftOk(sender, uuids[0]).at("/giftId").asLong()
            val admin = newAdmin()
            adminUserStatus(admin, sender.id!!, "SUSPENDED").andExpect { status { is2xxSuccessful() } }
            assertEquals(TicketGiftCancelReason.SENDER_WITHDRAWN, giftOf(g1).cancelReason)
            adminUserStatus(admin, sender.id!!, "NORMAL").andExpect { status { is2xxSuccessful() } }
            val g2 = giftOk(sender, uuids[1]).at("/giftId").asLong()
            adminUserStatus(admin, sender.id!!, "FORBIDDEN").andExpect { status { is2xxSuccessful() } }
            assertEquals(TicketGiftCancelReason.SENDER_WITHDRAWN, giftOf(g2).cancelReason)
        }

        @Test
        fun `받은 사람 탈퇴 - 선물 기록·티켓 그대로 (기존 탈퇴 정책)`() {
            val shop = Shop()
            val q = pendingAndAccepted(shop, newBuyer())
            userDomainService.withDrawUser(q.receiver.id!!)
            assertEquals(TicketGiftStatus.ACCEPTED, giftOf(q.acceptedGiftId).status)
            assertEquals(TicketGiftStatus.PENDING, giftOf(q.pendingGiftId).status)
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, ticketByUuid(q.receiverUuid).issuedTicketStatus)
        }

        @Test
        fun `운영 공연 삭제·준비중 전환 - 대기 선물 CANCELED(EVENT_REMOVED)`() {
            val admin = newAdmin()
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0])
            val g = created.at("/giftId").asLong()
            adminDeleteEvent(admin, shop.eventId).andExpect { status { is2xxSuccessful() } }
            assertEquals(TicketGiftCancelReason.EVENT_REMOVED, giftOf(g).cancelReason)
            // 삭제된 공연은 조회되지 않지만(@Where) 랜딩은 404 가 아니라 취소된 선물 화면
            val landed = landing(null, created.at("/giftToken").asText())
            assertEquals("CANCELED", landed.at("/viewState").asText())
            assertTrue(landed.at("/event").isNull)

            val shop2 = Shop()
            val sender2 = newBuyer()
            val (_, uuids2) = approvedOrder(shop2, sender2)
            val g2 = giftOk(sender2, uuids2[0]).at("/giftId").asLong()
            adminEventStatus(admin, shop2.eventId, "PREPARING").andExpect { status { is2xxSuccessful() } }
            assertEquals(TicketGiftCancelReason.EVENT_REMOVED, giftOf(g2).cancelReason)
        }

        @Test
        fun `운영 상태 변경 정산중·지난공연 - 대기 선물 그대로 (선물 만료로 보임, 회수 가능)`() {
            val admin = newAdmin()
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0])
            adminEventStatus(admin, shop.eventId, "CLOSED").andExpect { status { is2xxSuccessful() } }
            val g = giftOf(created.at("/giftId").asLong())
            assertEquals(TicketGiftStatus.PENDING, g.status)
            assertEquals("EXPIRED", landing(null, created.at("/giftToken").asText()).at("/viewState").asText())
            assertTrue(myTicket(sender, uuids[0]).andExpect { status { isOk() } }.data().at("/isGiftExpired").asBoolean())
            cancelGift(sender, g.id!!).andExpect { status { isOk() } }
        }
    }

    data class Quad(
        val orderUuid: String,
        val pendingGiftId: Long,
        val acceptedGiftId: Long,
        val receiver: band.gosrock.domain.domains.user.domain.User,
        val receiverUuid: String,
    )

    @Nested
    @DisplayName("알림 (보낸 사람: 생성·수락·거절·반환 / 받은 사람: 수락·선물받은 티켓 취소 / 회수 없음)")
    inner class Notifications {

        @Test
        fun `생성 GIFT_SENT, 수락 GIFT_ACCEPTED·GIFT_RECEIVED, 반환 GIFT_RETURNED — 대상·딥링크`() {
            val shop = Shop()
            val sender = newBuyer("보낸이")
            val receiver = newBuyer("받은이")
            val (_, uuids) = approvedOrder(shop, sender)
            val (giftId, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            assertEquals(1, awaitNotification(sender, NotificationType.GIFT_SENT))
            assertEquals(1, awaitNotification(sender, NotificationType.GIFT_ACCEPTED))
            assertEquals(1, awaitNotification(receiver, NotificationType.GIFT_RECEIVED))
            assertEquals(0, notificationCount(receiver, NotificationType.GIFT_SENT))
            assertEquals(0, notificationCount(sender, NotificationType.GIFT_RECEIVED))
            val accepted = notificationRepository.findAllByUserId(sender.id!!).single { it.type == NotificationType.GIFT_ACCEPTED }
            assertEquals("GIFT", accepted.targetType.name)
            assertEquals(giftId.toString(), accepted.targetId)
            assertEquals(shop.eventId, accepted.eventId)
            assertTrue(accepted.body.contains("받은이 님이"), accepted.body)
            assertTrue(notificationRepository.findAllByUserId(receiver.id!!).single { it.type == NotificationType.GIFT_RECEIVED }.body.contains("보낸이 님에게"))
            returnTicket(receiver, newUuid).andExpect { status { isOk() } }
            assertEquals(1, awaitNotification(sender, NotificationType.GIFT_RETURNED))
        }

        @Test
        fun `거절 GIFT_REJECTED → 보낸 사람, 회수는 알림 없음`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            reject(newBuyer(), giftOk(sender, uuids[0]).at("/giftToken").asText()).andExpect { status { isOk() } }
            assertEquals(1, awaitNotification(sender, NotificationType.GIFT_REJECTED))
            val before = notificationRepository.findAllByUserId(sender.id!!).size
            cancelGift(sender, giftOk(sender, uuids[1]).at("/giftId").asLong()).andExpect { status { isOk() } }
            assertEquals(2, awaitNotification(sender, NotificationType.GIFT_SENT))
            Thread.sleep(500)
            // 회수 뒤 늘어난 것은 두 번째 생성 알림 1건뿐
            assertEquals(before + 1, notificationRepository.findAllByUserId(sender.id!!).size)
        }
    }

    @Nested
    @DisplayName("T-1 내 티켓 / G-7 선물 내역 / O-3 주문상세")
    inner class Views {

        @Test
        fun `T-1 - 주문 묶음, 상태별 개수, 선물 완료 행은 uuid 없음, 받은 티켓 묶음은 내 주문 아님, 승인 대기·거절 묶음`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            giftOk(sender, uuids[0])
            val other = approvedOrder(shop, newBuyer(), quantity = 1)
            val gifter = userRepository.findById(orderRepository.findByOrderUuid(other.first).get().userId!!).get()
            giveAndAccept(gifter, sender, other.second[0])
            val pendingApprove = v2OrderOk(sender, shopBody(shop)).at("/orderUuid").asText()
            val refused = v2OrderOk(sender, shopBody(shop, yes = false)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, refused, "SOLD_OUT").andExpect { status { isOk() } }

            val all = myTickets(sender)
            val groups = all.filter { it.at("/isMyOrder").asBoolean() }.associateBy { it.at("/orderUuid").asText() }
            val mine = groups.getValue(orderUuid)
            assertTrue(mine.at("/isMyOrder").asBoolean())
            assertEquals(1, mine.at("/counts/GIFT_PENDING").asInt())
            assertEquals(1, mine.at("/counts/APPROVED").asInt())
            assertEquals(listOf("PENDING", "NONE"), mine.at("/tickets").map { it.at("/giftState").asText() })
            val received = all.single { !it.at("/isMyOrder").asBoolean() }
            assertTrue(received.at("/orderUuid").isNull, "받은 티켓 묶음은 원 주문 uuid 를 숨긴다 (T-2 와 같은 기준)")
            assertEquals("ticket:" + received.at("/tickets/0/issuedTicketNo").asText(), received.at("/groupKey").asText())
            assertEquals(orderUuid, mine.at("/groupKey").asText())
            assertTrue(received.at("/orderNo").isNull)
            assertTrue(received.at("/orderStatus").isNull)
            assertEquals("RECEIVED", received.at("/tickets/0/state").asText())
            assertTrue(received.at("/tickets/0/isReceived").asBoolean())
            assertEquals(1, groups.getValue(pendingApprove).at("/counts/PENDING_APPROVE").asInt())
            assertEquals(0, groups.getValue(pendingApprove).at("/tickets").size())
            assertEquals("SOLD_OUT", groups.getValue(refused).at("/refuseReasonType").asText())
            assertEquals(1, groups.getValue(refused).at("/counts/REFUSED").asInt())

            // 보낸 사람(gifter)에게는 선물 완료 행 (uuid 없음)
            val sentGroup = myTickets(gifter).single { it.at("/orderUuid").asText() == other.first }
            assertEquals("GIFT_SENT", sentGroup.at("/tickets/0/state").asText())
            // 보낸 사람은 giftId 로 선물 완료 티켓 상세를 연다 (uuid·QR 없음)
            val sentDetail = v2Get(gifter, "/me/gifts/${sentGroup.at("/tickets/0/giftId").asLong()}/ticket").andExpect { status { isOk() } }.data()
            assertEquals("GIFT_SENT", sentDetail.at("/state").asText())
            assertEquals("SENT", sentDetail.at("/giftState").asText())
            assertTrue(sentDetail.at("/ticketUuid").isNull)
            assertTrue(sentDetail.at("/qrValue").isNull)
            assertFalse(sentDetail.at("/canGift").asBoolean())
            assertEquals(other.first, sentDetail.at("/orderUuid").asText())
            assertTrue(sentDetail.at("/gift/receiverName").asText().isNotEmpty())
            assertEquals("Gift_404_1", v2Get(sender, "/me/gifts/${sentGroup.at("/tickets/0/giftId").asLong()}/ticket").andExpect { status { isNotFound() } }.code())
            assertEquals("SENT", sentGroup.at("/tickets/0/giftState").asText())
            assertTrue(sentGroup.at("/tickets/0/ticketUuid").isNull)
            assertEquals(1, sentGroup.at("/counts/GIFT_SENT").asInt())
            assertTrue(myTickets(receiver).isEmpty)
        }

        @Test
        fun `T-1 정렬 - 종료 전 공연 시작 임박순, 지난 공연은 뒤 (최근 시작 순), 선물 만료 표시`() {
            val buyer = newBuyer()
            val far = Shop()
            val near = Shop()
            val past = Shop()
            val farOrder = approvedOrder(far, buyer).first
            val nearOrder = approvedOrder(near, buyer).first
            val (pastOrder, pastUuids) = approvedOrder(past, buyer)
            giftOk(buyer, pastUuids[0])
            setEventStart(near.eventId, java.time.LocalDateTime.now().plusDays(1))
            endEvent(past.eventId)
            val groups = myTickets(buyer)
            assertEquals(listOf(nearOrder, farOrder, pastOrder), groups.map { it.at("/orderUuid").asText() })
            assertEquals("GIFT_EXPIRED", groups[2].at("/tickets/0/state").asText())
            assertTrue(groups[2].at("/tickets/0/isGiftExpired").asBoolean())
        }

        @Test
        fun `선물 완료 상세 - 반환·대기 중인 선물은 Gift_404_1 (돌아온 티켓은 T-2), 남의 선물 404`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val (giftId, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            v2Get(sender, "/me/gifts/$giftId/ticket").andExpect { status { isOk() } }
            assertEquals("Gift_404_1", v2Get(receiver, "/me/gifts/$giftId/ticket").andExpect { status { isNotFound() } }.code())
            returnTicket(receiver, newUuid).andExpect { status { isOk() } }
            assertEquals("Gift_404_1", v2Get(sender, "/me/gifts/$giftId/ticket").andExpect { status { isNotFound() } }.code())
            val pending = giftOk(sender, uuids[1]).at("/giftId").asLong()
            assertEquals("Gift_404_1", v2Get(sender, "/me/gifts/$pending/ticket").andExpect { status { isNotFound() } }.code())
        }

        @Test
        fun `v1 주문 상세 환불 가능 표시 - 선물 대기·완료면 false, 선물 없으면 기존 값(true), 회수하면 다시 true`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender)
            fun refundable() = mockMvc.get("/api/v1/orders/$orderUuid") { with(auth(sender)) }
                .andExpect { status { isOk() } }.data().at("/refundInfo/availAble").asBoolean()
            fun hostRefundable() = mockMvc.get("/api/v1/events/${shop.eventId}/orders/$orderUuid") { with(auth(shop.team.master)) }
                .andExpect { status { isOk() } }.data().at("/refundInfo/availAble").asBoolean()
            assertTrue(refundable())
            val giftId = giftOk(sender, uuids[0]).at("/giftId").asLong()
            assertFalse(refundable())
            // v1 사용자 주문 목록·최근 주문도 같은 기준, 호스트 주문 상세는 선물 반영 없음
            assertFalse(mockMvc.get("/api/v1/orders/recent") { with(auth(sender)) }.andExpect { status { isOk() } }.data().at("/refundInfo/availAble").asBoolean())
            assertTrue(hostRefundable(), "호스트 상세는 사용자 환불 가능 표시에 선물을 반영하지 않는다")
            cancelGift(sender, giftId).andExpect { status { isOk() } }
            assertTrue(refundable())
            giveAndAccept(sender, newBuyer(), uuids[0])
            assertFalse(refundable())
        }

        @Test
        fun `G-7 - 보낸 선물(메모·링크는 대기 중만), 받은 선물(메모 없음, 보낸 사람 닉네임)`() {
            val shop = Shop()
            val sender = newBuyer("보낸이")
            val receiver = newBuyer("받은이")
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            giftOk(sender, uuids[0], "대기메모")
            val acceptedToken = giftOk(sender, uuids[1], "완료메모").at("/giftToken").asText()
            accept(receiver, acceptedToken).andExpect { status { isOk() } }

            val sent = gifts(sender, "SENT").at("/content")
            assertEquals(2, sent.size())
            assertEquals("ACCEPTED", sent[0].at("/status").asText())
            assertEquals("완료메모", sent[0].at("/memo").asText())
            assertEquals("받은이", sent[0].at("/counterpartName").asText())
            assertTrue(sent[0].at("/giftToken").isNull)
            assertEquals("PENDING", sent[1].at("/status").asText())
            assertTrue(sent[1].at("/linkPath").asText().startsWith("/gifts/"))
            val received = gifts(receiver, "RECEIVED").at("/content")
            assertEquals(1, received.size())
            assertTrue(received[0].at("/memo").isNull)
            assertTrue(received[0].at("/giftToken").isNull)
            assertEquals("보낸이", received[0].at("/counterpartName").asText())
            assertEquals(0, gifts(receiver, "SENT").at("/content").size())
        }

        @Test
        fun `O-3 - 선물 대기 행(PENDING, uuid 있음), 선물 완료 행(SENT, uuid 없음)`() {
            val shop = Shop()
            val sender = newBuyer()
            val (orderUuid, uuids) = approvedOrder(shop, sender, quantity = 2)
            val pendingId = giftOk(sender, uuids[0]).at("/giftId").asLong()
            val (acceptedId, _) = giveAndAccept(sender, newBuyer(), uuids[1])
            val rows = myOrder(sender, orderUuid).andExpect { status { isOk() } }.data().at("/issuedTickets")
            assertEquals(2, rows.size())
            assertEquals("PENDING", rows[0].at("/giftState").asText())
            assertEquals(uuids[0], rows[0].at("/ticketUuid").asText())
            assertEquals(pendingId, rows[0].at("/giftId").asLong())
            assertEquals("SENT", rows[1].at("/giftState").asText())
            assertTrue(rows[1].at("/ticketUuid").isNull)
            assertEquals(acceptedId, rows[1].at("/giftId").asLong())
        }
    }

    @Nested
    @DisplayName("동시성 (주문 락 → 티켓 행 잠금)")
    inner class Concurrency {

        @Test
        fun `같은 링크 동시 수락 5명 - 1명만 성공, 나머지 Gift_400_3, 소유자 1명, uuid 1번만 교체`() {
            val shop = Shop()
            val sender = newBuyer()
            val (_, uuids) = approvedOrder(shop, sender)
            val created = giftOk(sender, uuids[0])
            val token = created.at("/giftToken").asText()
            val racers = (1..5).map { newBuyer("경합$it") }
            val results = concurrently(*racers.map { r -> { accept(r, token).outcome() } }.toTypedArray())
            assertEquals(1, results.count { it == "OK" }, results.toString())
            assertEquals(4, results.count { it == "Gift_400_3" }, results.toString())
            val gift = giftOf(created.at("/giftId").asLong())
            val ticket = ticketOf(gift.issuedTicketId)
            assertEquals(gift.receiverUserId, ticket.getUserId())
            assertTrue(gift.receiverUserId in racers.map { it.id })
            assertEquals(1, awaitNotification(sender, NotificationType.GIFT_ACCEPTED))
        }

        @Test
        fun `수락 ↔ 회수 동시 - 정확히 한쪽만 성공, 상태와 소유자가 맞는다`() {
            repeat(3) {
                val shop = Shop()
                val sender = newBuyer()
                val receiver = newBuyer()
                val (_, uuids) = approvedOrder(shop, sender)
                val created = giftOk(sender, uuids[0])
                val giftId = created.at("/giftId").asLong()
                val (a, c) = concurrently(
                    { "A:" + accept(receiver, created.at("/giftToken").asText()).outcome() },
                    { "C:" + cancelGift(sender, giftId).outcome() },
                ).sorted()
                assertEquals(1, listOf(a, c).count { it.endsWith(":OK") }, "$a $c")
                val gift = giftOf(giftId)
                val ticket = ticketOf(gift.issuedTicketId)
                if (a == "A:OK") {
                    assertEquals(TicketGiftStatus.ACCEPTED, gift.status)
                    assertEquals(receiver.id, ticket.getUserId())
                    assertEquals("C:Gift_400_3", c)
                } else {
                    assertEquals(TicketGiftStatus.CANCELED, gift.status)
                    assertEquals(sender.id, ticket.getUserId())
                    assertEquals(uuids[0], ticket.uuid)
                    assertEquals("A:Gift_400_3", a)
                }
            }
        }

        @Test
        fun `수락 ↔ 호스트 취소 동시 - 어느 쪽이 먼저든 주문·티켓 취소, 선물은 (ACCEPTED + 받은 사람) 또는 (CANCELED(ORDER_CANCELED) + 보낸 사람)`() {
            repeat(3) {
                val shop = Shop()
                val sender = newBuyer()
                val receiver = newBuyer()
                val (orderUuid, uuids) = approvedOrder(shop, sender)
                val created = giftOk(sender, uuids[0])
                val (a, c) = concurrently(
                    { "A:" + accept(receiver, created.at("/giftToken").asText()).outcome() },
                    { "C:" + v2HostCancel(shop.team.manager, shop.eventId, orderUuid).outcome() },
                ).sorted()
                assertEquals("C:OK", c)
                val gift = giftOf(created.at("/giftId").asLong())
                val ticket = ticketOf(gift.issuedTicketId)
                assertEquals(OrderStatus.CANCELED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
                assertEquals(IssuedTicketStatus.CANCELED, ticket.issuedTicketStatus)
                if (a == "A:OK") {
                    assertEquals(TicketGiftStatus.ACCEPTED, gift.status)
                    assertEquals(receiver.id, ticket.getUserId())
                    assertEquals(1, awaitNotification(receiver, NotificationType.GIFT_TICKET_CANCELED))
                } else {
                    assertEquals("A:Gift_400_3", a)
                    assertEquals(TicketGiftCancelReason.ORDER_CANCELED, gift.cancelReason)
                    assertEquals(sender.id, ticket.getUserId())
                }
            }
        }

        @Test
        fun `생성 ↔ 사용자 취소(O-4) 동시 - 둘 다 성공하는 일은 없다`() {
            repeat(3) {
                val shop = Shop()
                val free = freeTicket(shop, approvalRequired = false)
                val sender = newBuyer()
                val orderUuid = v2OrderOk(sender, freeBodyOf(shop, free)).at("/orderUuid").asText()
                val ticketUuid = shop.ticketUuids(orderUuid)[0]
                val (c, g) = concurrently(
                    { "G:" + gift(sender, ticketUuid).outcome() },
                    { "C:" + cancelMy(sender, orderUuid).outcome() },
                ).sorted()
                assertEquals(1, listOf(c, g).count { it.endsWith(":OK") }, "$c $g")
                val pending = ticketGiftRepository.findAllByIssuedTicketIdIn(listOf(ticketByUuid(ticketUuid).id!!)).filter { it.isPending() }
                val order = orderRepository.findByOrderUuid(orderUuid).get()
                if (g == "G:OK") {
                    assertEquals(1, pending.size)
                    assertEquals(OrderStatus.APPROVED, order.orderStatus)
                    assertEquals("C:Order_400_24", c)
                } else {
                    assertEquals(0, pending.size)
                    assertEquals(OrderStatus.REFUND, order.orderStatus)
                    assertEquals("G:Gift_400_1", g)
                }
            }
        }
    }

    @Nested
    @DisplayName("T-3 티켓탭 공지 바 (A8: 승인 알림을 읽거나 그 주문의 티켓을 열면 해제)")
    inner class NewApproved {

        private fun newApproved(user: band.gosrock.domain.domains.user.domain.User) =
            v2Get(user, "/me/tickets/new-approved").andExpect { status { isOk() } }.data()

        /** 공지 바가 조건을 만족할 때까지 기다린다 (읽음 처리는 비동기) */
        private fun awaitBar(user: band.gosrock.domain.domains.user.domain.User, until: (com.fasterxml.jackson.databind.JsonNode) -> Boolean) {
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && !until(newApproved(user))) Thread.sleep(50)
            assertTrue(until(newApproved(user)), newApproved(user).toString())
        }

        /** 승인 알림이 저장될 때까지 기다린 뒤 공지 바 */
        private fun approvedAndNotified(shop: Shop, buyer: band.gosrock.domain.domains.user.domain.User): Pair<String, List<String>> =
            approvedOrder(shop, buyer).also { assertEquals(1, awaitNotification(buyer, NotificationType.ORDER_APPROVED)) }

        @Test
        fun `승인되면 hasNew + 주문, 승인 알림을 읽으면(N-3) 해제, 티켓탭 진입(T-1)만으로는 해제되지 않음`() {
            val shop = Shop()
            val buyer = newBuyer()
            assertFalse(newApproved(buyer).at("/hasNew").asBoolean())
            val (orderUuid, _) = approvedAndNotified(shop, buyer)
            val bar = newApproved(buyer)
            assertTrue(bar.at("/hasNew").asBoolean())
            assertEquals(listOf(orderUuid), bar.at("/orderUuids").map { it.asText() })
            myTickets(buyer)
            assertTrue(newApproved(buyer).at("/hasNew").asBoolean())
            v2Post(buyer, "/me/notifications/read", mapOf("all" to true)).andExpect { status { isOk() } }
            assertFalse(newApproved(buyer).at("/hasNew").asBoolean())
        }

        @Test
        fun `그 주문의 티켓을 열면(T-2) 그 주문만 해제, 다른 승인 주문은 남음, 멱등`() {
            val shop = Shop()
            val buyer = newBuyer()
            val (first, firstUuids) = approvedAndNotified(shop, buyer)
            val second = v2OrderOk(buyer, shopBody(shop, yes = false)).at("/orderUuid").asText()
                .also { v1Approve(shop.team.master, shop.eventId, it).andExpect { status { isOk() } } }
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && notificationCount(buyer, NotificationType.ORDER_APPROVED) < 2) Thread.sleep(50)
            assertEquals(listOf(second, first), newApproved(buyer).at("/orderUuids").map { it.asText() })

            myTicket(buyer, firstUuids[0]).andExpect { status { isOk() } }
            // 읽음 처리는 조회 커밋 뒤 알림 전용 풀에서 (비동기)
            awaitBar(buyer) { it.at("/orderUuids").map { u -> u.asText() } == listOf(second) }
            myTicket(buyer, firstUuids[0]).andExpect { status { isOk() } }
            assertEquals(listOf(second), newApproved(buyer).at("/orderUuids").map { it.asText() })
            assertTrue(notificationRepository.findAllByUserId(buyer.id!!).single { it.targetId == first }.isRead)
            assertFalse(notificationRepository.findAllByUserId(buyer.id!!).single { it.targetId == second }.isRead)
        }

        @Test
        fun `승인 뒤 호스트가 취소한 주문은 공지 바에서 빠짐, 남의 승인은 안 보임`() {
            val shop = Shop()
            val buyer = newBuyer()
            val (orderUuid, _) = approvedAndNotified(shop, buyer)
            v2HostCancel(shop.team.manager, shop.eventId, orderUuid).andExpect { status { isOk() } }
            assertFalse(newApproved(buyer).at("/hasNew").asBoolean())
            assertFalse(newApproved(newBuyer()).at("/hasNew").asBoolean())
        }

        @Test
        fun `받은 사람이 선물받은 티켓을 열어도 보낸 사람(주문자)의 공지 바는 그대로`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            val (_, uuids) = approvedAndNotified(shop, sender)
            val (_, newUuid) = giveAndAccept(sender, receiver, uuids[0])
            myTicket(receiver, newUuid).andExpect { status { isOk() } }
            Thread.sleep(500)
            assertTrue(newApproved(sender).at("/hasNew").asBoolean())
            assertFalse(newApproved(receiver).at("/hasNew").asBoolean())
        }

        @Test
        fun `비로그인은 401`() {
            v2Get(null, "/me/tickets/new-approved").andExpect { status { isUnauthorized() } }
        }
    }

    @Nested
    @DisplayName("1인 매수 제한은 원 구매자 기준 (A3, 기본안 18)")
    inner class PurchaseLimit {

        @Test
        fun `보낸 사람은 선물해도 제한이 풀리지 않고, 받은 사람은 받은 티켓이 제한에 들어가지 않는다 (v2 주문·v1 장바구니)`() {
            val shop = Shop()
            val sender = newBuyer()
            val receiver = newBuyer()
            // 1인 4장: 보낸 사람 2 + 1 = 3장 구매, 그중 2장 선물 (H2 는 승인 직후 발급분까지 세므로 두 번째 승인은 1장 — V2OperationTestSupport 주의)
            val (_, a) = approvedOrder(shop, sender, quantity = 2)
            approvedOrder(shop, sender, quantity = 1)
            giveAndAccept(sender, receiver, a[0])
            giveAndAccept(sender, receiver, a[1])
            // 보낸 사람: 산 3장으로 센다 → 2장 더는 1인 제한 (현재 소유자 기준이었다면 1 + 2 = 3 으로 통과)
            // (yes = false: 앞 주문과 같은 요청이면 10초 중복 요청으로 앞 주문을 돌려준다)
            assertEquals("Ticket_Item_400_6", v2CreateOrder(sender, shopBody(shop, quantity = 2, yes = false)).andExpect { status { isBadRequest() } }.code())
            v1Cart(sender, shop.ticketId, 2, v1Answers(sender, shop.eventId, shop.ticketId)).andExpect { status { isBadRequest() } }
            v1Cart(sender, shop.ticketId, 1, v1Answers(sender, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }
            // 받은 사람: 받은 2장은 세지 않는다 → v1 장바구니 4장 가능 (현재 소유자 기준이었다면 2 + 4 = 6 으로 실패)
            v1Cart(receiver, shop.ticketId, 4, v1Answers(receiver, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }
            approvedOrder(shop, receiver, quantity = 2)
        }
    }
}
