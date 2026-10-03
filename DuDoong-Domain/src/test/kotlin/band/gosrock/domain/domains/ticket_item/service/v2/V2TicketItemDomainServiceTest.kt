package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.CannotModifyEndedEventException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.exception.EmptyAccountInfoException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenSoldTicketItemChangeException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketPriceException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketSalePeriodException
import band.gosrock.domain.domains.ticket_item.exception.TicketDisabledEventException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotOnSaleException
import band.gosrock.domain.domains.ticket_item.exception.UnsupportedV2TicketPayTypeException
import band.gosrock.domain.domains.ticket_item.service.TicketItemService
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem.Companion.NO_PURCHASE_LIMIT
import band.gosrock.domain.domains.ticket_item.domain.TicketItem.Companion.UNLIMITED_SUPPLY_COUNT
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemFieldException
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.springframework.test.util.ReflectionTestUtils

/** v2 티켓 도메인 규칙 (#707): 판매 상태 판정, 생성 검증, 판매 기간, 판매된 티켓 수정 허용 필드 */
class V2TicketItemDomainServiceTest {

    private val v2EventDomainService = V2EventDomainService(
        eventRepository = mock(EventRepository::class.java),
        eventService = mock(EventService::class.java),
        ticketItemAdaptor = mock(TicketItemAdaptor::class.java),
        tagAdaptor = mock(TagAdaptor::class.java),
    )

    private val service = V2TicketItemDomainService(
        ticketItemAdaptor = mock(TicketItemAdaptor::class.java),
        optionGroupAdaptor = mock(OptionGroupAdaptor::class.java),
        eventAdaptor = mock(EventAdaptor::class.java),
        orderAdaptor = mock(OrderAdaptor::class.java),
        ticketItemService = mock(TicketItemService::class.java),
        v2EventDomainService = v2EventDomainService,
    )

    private val eventStart: LocalDateTime = LocalDateTime.of(2030, 5, 11, 18, 0)
    private val now: LocalDateTime = LocalDateTime.of(2030, 1, 1, 12, 0)

    private fun event(hasTicket: Boolean = true, status: EventStatus = EventStatus.PREPARING): Event =
        v2EventDomainService.newEvent(hostId = 1L, name = "정기공연", startAt = eventStart, endAt = eventStart.plusHours(2), hasTicket = hasTicket)
            .also { ReflectionTestUtils.setField(it, "status", status) }

    private val account = AccountInfoVo(bankName = "신한은행", accountNumber = "110-123", accountHolder = "고스락")

    private fun dudoongForm(
        price: Long = 6000,
        supplyCount: Long? = 100,
        account: AccountInfoVo? = this.account,
        approvalRequired: Boolean = true,
        saleStartAt: LocalDateTime? = null,
        saleEndAt: LocalDateTime? = null,
    ) = V2TicketItemForm(
        payType = TicketPayType.DUDOONG_TICKET,
        name = "일반",
        description = "일반 입장",
        price = price,
        supplyCount = supplyCount,
        account = account,
        approvalRequired = approvalRequired,
        isQuantityPublic = supplyCount != null,
        purchaseLimit = 4,
        saleStartAt = saleStartAt,
        saleEndAt = saleEndAt,
    )

    private fun freeForm(approvalRequired: Boolean = false, price: Long = 0) = dudoongForm(price = price, account = account, approvalRequired = approvalRequired)
        .copy(payType = TicketPayType.FREE_TICKET)

    private fun TicketItem.sold(count: Long): TicketItem = also { it.reduceQuantity(count) }

    @Nested
    inner class SaleState {

        @Test
        fun `재고 감소 없음 BEFORE_SALE, 감소 SOLD, 판매 중단은 판매 여부와 무관하게 SUSPENDED`() {
            val item = service.newTicketItem(event(), dudoongForm(), now)
            assertEquals(V2TicketSaleState.BEFORE_SALE, service.saleState(item))
            item.sold(1)
            assertEquals(V2TicketSaleState.SOLD, service.saleState(item))
            item.isSellable = false
            assertEquals(V2TicketSaleState.SUSPENDED, service.saleState(item))
            assertTrue(item.isSold())
        }

        @Test
        fun `구매 가능은 판매 중 + 판매 기간 + 공연 OPEN + 시작 전 + 재고`() {
            val open = event(status = EventStatus.OPEN)
            val item = service.newTicketItem(open, dudoongForm(supplyCount = 1), now)
            assertTrue(service.isPurchasable(item, open, now))
            assertFalse(service.isPurchasable(item, event(status = EventStatus.PREPARING), now))
            assertFalse(service.isPurchasable(item, open, eventStart))
            item.isSellable = false
            assertFalse(service.isPurchasable(item, open, now))
            item.isSellable = true
            item.saleStartAt = now.plusMinutes(1)
            assertFalse(service.isPurchasable(item, open, now))
            item.saleStartAt = null
            item.saleEndAt = now
            assertFalse(service.isPurchasable(item, open, now))
            item.saleEndAt = null
            item.sold(1)
            assertFalse(service.isPurchasable(item, open, now))
        }

        @Test
        fun `엔티티 판매 중 판정 - v1 티켓(isSellable true 또는 null, 기간 null)은 항상 판매 중`() {
            val v1 = TicketItem(isSellable = true, quantity = 1, supplyCount = 1)
            assertTrue(v1.isOnSale(now))
            assertTrue(TicketItem(isSellable = null).isOnSale(now))
            assertDoesNotThrow { v1.validateOnSale(now) }
            val suspended = TicketItem(isSellable = false)
            assertThrows<TicketItemNotOnSaleException> { suspended.validateOnSale(now) }
            val period = TicketItem(isSellable = true, saleStartAt = now, saleEndAt = now.plusHours(1))
            assertTrue(period.isOnSale(now))
            assertFalse(period.isOnSale(now.minusMinutes(1)))
            assertFalse(period.isOnSale(now.plusHours(1)))
        }
    }

    @Nested
    inner class Create {

        @Test
        fun `두둥티켓은 항상 승인, 계좌 저장, 무제한-매수제한 없음은 저장값으로, 무제한이면 재고 공개 끔`() {
            val item = service.newTicketItem(event(), dudoongForm(supplyCount = null, approvalRequired = false).copy(purchaseLimit = null), now)
            assertEquals(TicketType.APPROVAL, item.type)
            assertEquals("신한은행", item.accountInfo!!.bankName)
            assertEquals(UNLIMITED_SUPPLY_COUNT, item.supplyCount)
            assertEquals(UNLIMITED_SUPPLY_COUNT, item.quantity)
            assertEquals(NO_PURCHASE_LIMIT, item.purchaseLimit)
            assertTrue(item.isUnlimitedSupply())
            assertTrue(item.hasNoPurchaseLimit())
            assertFalse(item.isQuantityPublic!!)
            assertTrue(item.isSellable!!)
            // 무제한인데 재고 공개를 켜면 400
            assertThrows<InvalidTicketItemFieldException> {
                service.newTicketItem(event(), dudoongForm(supplyCount = null).copy(isQuantityPublic = true), now)
            }
            // v1 결제 방식 검증도 통과
            assertDoesNotThrow { item.validateTicketPayType(false) }
        }

        @Test
        fun `무료티켓은 승인 선택, 계좌는 저장하지 않음, v1 검증 통과`() {
            val fcfs = service.newTicketItem(event(), freeForm(approvalRequired = false), now)
            assertEquals(TicketType.FIRST_COME_FIRST_SERVED, fcfs.type)
            assertNull(fcfs.accountInfo)
            assertEquals(Money.ZERO, fcfs.price)
            assertDoesNotThrow { fcfs.validateTicketPayType(false) }
            val approval = service.newTicketItem(event(), freeForm(approvalRequired = true), now)
            assertEquals(TicketType.APPROVAL, approval.type)
            assertDoesNotThrow { approval.validateTicketPayType(false) }
        }

        @Test
        fun `티켓 없음 공연, PRICE, 가격 규칙, 계좌 누락, 종료 공연은 실패`() {
            assertThrows<TicketDisabledEventException> { service.newTicketItem(event(hasTicket = false), dudoongForm(), now) }
            assertThrows<UnsupportedV2TicketPayTypeException> {
                service.newTicketItem(event(), dudoongForm().copy(payType = TicketPayType.PRICE_TICKET), now)
            }
            assertThrows<InvalidTicketPriceException> { service.newTicketItem(event(), dudoongForm(price = 0), now) }
            assertThrows<InvalidTicketPriceException> { service.newTicketItem(event(), freeForm(price = 1000), now) }
            assertThrows<EmptyAccountInfoException> { service.newTicketItem(event(), dudoongForm(account = null), now) }
            assertThrows<EmptyAccountInfoException> {
                service.newTicketItem(event(), dudoongForm(account = AccountInfoVo("신한", " ", "고스락")), now)
            }
            assertThrows<CannotModifyEndedEventException> { service.newTicketItem(event(status = EventStatus.CLOSED), dudoongForm(), now) }
            assertDoesNotThrow { service.newTicketItem(event(status = EventStatus.OPEN), dudoongForm(), now) }
            // 가격 상한 10,000,000원, 이름 12자·설명 30자
            assertDoesNotThrow { service.newTicketItem(event(), dudoongForm(price = 10_000_000), now) }
            assertThrows<InvalidTicketPriceException> { service.newTicketItem(event(), dudoongForm(price = 10_000_001), now) }
            assertThrows<InvalidTicketItemFieldException> { service.newTicketItem(event(), dudoongForm().copy(name = "가".repeat(13)), now) }
            assertThrows<InvalidTicketItemFieldException> { service.newTicketItem(event(), dudoongForm().copy(name = "   "), now) }
            assertThrows<InvalidTicketItemFieldException> { service.newTicketItem(event(), dudoongForm().copy(description = "가".repeat(31)), now) }
            assertDoesNotThrow { service.newTicketItem(event(), dudoongForm().copy(name = "  ${"가".repeat(12)}  "), now) }
        }
    }

    @Nested
    inner class SalePeriod {

        @Test
        fun `시작은 종료 전, 둘 다 공연 시작 이전(종료는 같아도 됨), 새 종료는 현재 이후`() {
            val e = event()
            assertThrows<InvalidTicketSalePeriodException> {
                service.newTicketItem(e, dudoongForm(saleStartAt = now.plusDays(2), saleEndAt = now.plusDays(1)), now)
            }
            assertThrows<InvalidTicketSalePeriodException> {
                service.newTicketItem(e, dudoongForm(saleStartAt = now.plusDays(1), saleEndAt = now.plusDays(1)), now)
            }
            assertThrows<InvalidTicketSalePeriodException> { service.newTicketItem(e, dudoongForm(saleEndAt = eventStart.plusMinutes(1)), now) }
            assertThrows<InvalidTicketSalePeriodException> { service.newTicketItem(e, dudoongForm(saleStartAt = eventStart), now) }
            assertThrows<InvalidTicketSalePeriodException> { service.newTicketItem(e, dudoongForm(saleEndAt = now), now) }
            val item = service.newTicketItem(e, dudoongForm(saleStartAt = now.minusDays(1), saleEndAt = eventStart), now)
            assertEquals(eventStart, item.saleEndAt)
        }

        @Test
        fun `이미 지난 기존 종료값은 그대로 두면 다른 필드를 고칠 수 있다`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(saleEndAt = now.plusDays(1)), now)
            val later = now.plusDays(2)
            assertDoesNotThrow { service.applyUpdate(item, e, dudoongForm(saleEndAt = now.plusDays(1)).copy(description = "변경"), false, later) }
            assertThrows<InvalidTicketSalePeriodException> { service.applyUpdate(item, e, dudoongForm(saleEndAt = now.plusDays(1).plusMinutes(1)), false, later) }
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `판매 전이면 종류-이름-가격-계좌-승인-수량 모두 바꿀 수 있다`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(supplyCount = 100), now)
            service.applyUpdate(item, e, freeForm(approvalRequired = false).copy(name = "무료", supplyCount = 10), false, now)
            assertEquals(TicketPayType.FREE_TICKET, item.payType)
            assertEquals("무료", item.name)
            assertEquals(Money.ZERO, item.price)
            assertNull(item.accountInfo)
            assertEquals(TicketType.FIRST_COME_FIRST_SERVED, item.type)
            assertEquals(10L, item.supplyCount)
            assertEquals(10L, item.quantity)
        }

        @Test
        fun `판매됨이면 설명-판매기간-재고공개-매수제한-수량 증가만, 판매 수량은 유지`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(supplyCount = 10), now).sold(3)
            service.applyUpdate(
                item, e,
                dudoongForm(supplyCount = 20, saleStartAt = now.plusHours(1), saleEndAt = now.plusDays(1))
                    .copy(description = "새 설명", isQuantityPublic = false, purchaseLimit = null),
                false,
                now,
            )
            assertEquals("새 설명", item.description)
            assertEquals(now.plusHours(1), item.saleStartAt)
            assertFalse(item.isQuantityPublic!!)
            assertEquals(NO_PURCHASE_LIMIT, item.purchaseLimit)
            assertEquals(20L, item.supplyCount)
            assertEquals(17L, item.quantity)
            // 수량 지정 → 무제한도 증가
            service.applyUpdate(item, e, dudoongForm(supplyCount = null), false, now)
            assertEquals(UNLIMITED_SUPPLY_COUNT - 3, item.quantity)
            assertTrue(item.isUnlimitedSupply())
        }

        @Test
        fun `판매됨이면 잠긴 필드 변경과 수량 감소는 400, 같은 값은 허용`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(supplyCount = 10), now).sold(1)
            assertDoesNotThrow { service.applyUpdate(item, e, dudoongForm(supplyCount = 10), false, now) }
            listOf(
                dudoongForm().copy(name = "다른이름"),
                dudoongForm(price = 7000),
                dudoongForm(account = AccountInfoVo("국민은행", "110-123", "고스락")),
                dudoongForm(supplyCount = 9),
                freeForm(),
            ).forEach { form ->
                assertThrows<ForbiddenSoldTicketItemChangeException> { service.applyUpdate(item, e, form.copy(supplyCount = form.supplyCount), false, now) }
            }
            assertEquals("일반", item.name)
            assertEquals(10L, item.supplyCount)

            val free = service.newTicketItem(e, freeForm(approvalRequired = false), now).sold(1)
            assertThrows<ForbiddenSoldTicketItemChangeException> { service.applyUpdate(free, e, freeForm(approvalRequired = true), false, now) }
            // 무제한 → 수량 지정은 감소
            val unlimited = service.newTicketItem(e, dudoongForm(supplyCount = null), now).sold(1)
            assertThrows<ForbiddenSoldTicketItemChangeException> { service.applyUpdate(unlimited, e, dudoongForm(supplyCount = 999_999), false, now) }
        }

        @Test
        fun `유료 옵션이 붙은 티켓은 무료로 바꿀 수 없고, PRICE 티켓은 v2 수정 불가`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(), now)
            val option = OptionGroup(eventId = 1L, type = OptionGroupType.TRUE_FALSE, name = "뒷풀이", description = "d", isEssential = true)
                .createTicketOption(Money.wons(5000))
            ReflectionTestUtils.setField(option, "id", 7L)
            item.addItemOptionGroup(option)
            assertThrows<ForbiddenOptionPriceException> { service.applyUpdate(item, e, freeForm(), false, now) }

            val price = TicketItem(payType = TicketPayType.PRICE_TICKET, price = Money.wons(1000), quantity = 1, supplyCount = 1, type = TicketType.FIRST_COME_FIRST_SERVED)
            assertThrows<UnsupportedV2TicketPayTypeException> { service.applyUpdate(price, e, dudoongForm(), false, now) }
        }
    }

    @Nested
    inner class Lock {

        @Test
        fun `잠금 = 재고 감소 또는 승인 대기 주문`() {
            val item = service.newTicketItem(event(), dudoongForm(), now)
            assertFalse(service.isLocked(item, hasPendingOrders = false))
            assertTrue(service.isLocked(item, hasPendingOrders = true))
            item.sold(1)
            assertTrue(service.isLocked(item, hasPendingOrders = false))
        }

        @Test
        fun `승인 대기 주문만 있어도(재고 감소 없음) 가격·계좌·이름 변경과 수량 감소 400, 허용 필드는 수정`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(supplyCount = 10), now)
            listOf(dudoongForm(price = 7000), dudoongForm(account = AccountInfoVo("국민은행", "1", "a")), dudoongForm().copy(name = "변경"), dudoongForm(supplyCount = 9))
                .forEach { assertThrows<ForbiddenSoldTicketItemChangeException> { service.applyUpdate(item, e, it, true, now) } }
            service.applyUpdate(item, e, dudoongForm(supplyCount = 11).copy(description = "설명만"), true, now)
            assertEquals("설명만", item.description)
            assertEquals(11L, item.supplyCount)
            // 주문이 없으면 같은 변경 가능
            service.applyUpdate(item, e, dudoongForm(price = 7000, supplyCount = 5), false, now)
            assertEquals(Money.wons(7000), item.price)
        }

        @Test
        fun `v1 의 긴 이름·앞뒤 공백은 양쪽 trim 비교라 그대로 재전송하면 통과하고 저장값도 유지`() {
            val e = event()
            val longName = " 열세글자이름입니다아아아아 "
            val v1 = TicketItem(
                payType = TicketPayType.FREE_TICKET, name = longName, description = " v1 설명이 서른 자를 넘는 아주 긴 설명입니다 정말로 깁니다 ",
                price = Money.ZERO, quantity = 10, supplyCount = 10, purchaseLimit = 2, type = TicketType.FIRST_COME_FIRST_SERVED,
                isQuantityPublic = true, isSellable = true,
            ).sold(1)
            val resend = V2TicketItemForm(
                payType = TicketPayType.FREE_TICKET, name = longName, description = v1.description, price = 0, supplyCount = 10,
                account = null, approvalRequired = false, isQuantityPublic = false, purchaseLimit = 2, saleStartAt = null, saleEndAt = null,
            )
            service.applyUpdate(v1, e, resend, false, now)
            assertEquals(longName, v1.name)
            assertFalse(v1.isQuantityPublic!!)
            // 값을 바꾸면 길이 검증 (잠기지 않은 설명)
            assertThrows<InvalidTicketItemFieldException> { service.applyUpdate(v1, e, resend.copy(description = "가".repeat(31)), false, now) }
            service.applyUpdate(v1, e, resend.copy(description = "짧은 설명"), false, now)
            assertEquals("짧은 설명", v1.description)
        }

        @Test
        fun `현재 무제한이고 폼도 무제한이면 저장값(어드민 조정으로 더 클 수 있음)을 유지한다`() {
            val e = event()
            val item = service.newTicketItem(e, dudoongForm(supplyCount = null), now).sold(2)
            ReflectionTestUtils.setField(item, "supplyCount", UNLIMITED_SUPPLY_COUNT + 5)
            ReflectionTestUtils.setField(item, "quantity", UNLIMITED_SUPPLY_COUNT + 3)
            service.applyUpdate(item, e, dudoongForm(supplyCount = null).copy(description = "x"), false, now)
            assertEquals(UNLIMITED_SUPPLY_COUNT + 5, item.supplyCount)
            assertEquals(UNLIMITED_SUPPLY_COUNT + 3, item.quantity)
        }
    }
}
