package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.common.consts.DuDoongStatic.KR_NO
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.ticket_item.service.TicketItemService
import org.redisson.api.RedissonClient
import org.springframework.transaction.PlatformTransactionManager
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenLockedOptionChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.UnsupportedV2OptionTypeException
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.springframework.test.util.ReflectionTestUtils

/** v2 옵션 도메인 규칙 (#707, DEC-012) */
class V2TicketOptionDomainServiceTest {

    private val v2EventDomainService = V2EventDomainService(
        eventRepository = mock(EventRepository::class.java),
        eventService = mock(EventService::class.java),
        ticketItemAdaptor = mock(TicketItemAdaptor::class.java),
        tagAdaptor = mock(TagAdaptor::class.java),
    )

    private val v2TicketItemDomainService = V2TicketItemDomainService(
        ticketItemAdaptor = mock(TicketItemAdaptor::class.java),
        optionGroupAdaptor = mock(OptionGroupAdaptor::class.java),
        eventAdaptor = mock(EventAdaptor::class.java),
        orderAdaptor = mock(OrderAdaptor::class.java),
        ticketItemService = mock(TicketItemService::class.java),
        v2EventDomainService = v2EventDomainService,
    )

    private val service = V2TicketOptionDomainService(
        optionGroupAdaptor = mock(OptionGroupAdaptor::class.java),
        ticketItemAdaptor = mock(TicketItemAdaptor::class.java),
        eventAdaptor = mock(EventAdaptor::class.java),
        v2EventDomainService = v2EventDomainService,
        v2TicketItemDomainService = v2TicketItemDomainService,
        redissonClient = mock(RedissonClient::class.java),
        transactionManager = mock(PlatformTransactionManager::class.java),
    )

    private val start: LocalDateTime = LocalDateTime.of(2030, 5, 11, 18, 0)

    private fun event(): Event = v2EventDomainService.newEvent(1L, "공연", start, start.plusHours(2), true)

    private fun yesNo(price: Long = 0, id: Long = 1L): OptionGroup =
        service.newOptionGroup(event(), "뒷풀이", "참석하나요?", OptionGroupType.TRUE_FALSE, price)
            .also { ReflectionTestUtils.setField(it, "id", id) }

    private fun ticket(payType: TicketPayType = TicketPayType.DUDOONG_TICKET, vararg options: OptionGroup): TicketItem =
        TicketItem(payType = payType, price = Money.wons(if (payType == TicketPayType.FREE_TICKET) 0 else 1000), quantity = 10, supplyCount = 10, type = TicketType.APPROVAL)
            .also { item -> options.forEach { item.addItemOptionGroup(it) } }

    @Test
    fun `네-아니오는 v1 과 같이 예(추가금)-아니요(0) 2행, 주관식은 1행, 추가금 기본 0`() {
        val option = yesNo(price = 3000)
        assertEquals(listOf(KR_YES, KR_NO), option.options.map { it.answer })
        assertEquals(listOf(Money.wons(3000), Money.ZERO), option.options.map { it.additionalPrice })
        assertEquals(3000L, service.yesAdditionalPrice(option))
        assertTrue(option.isEssential!!)

        val subjective = service.newOptionGroup(event(), "이름", "입금자명", OptionGroupType.SUBJECTIVE, null)
        assertEquals(1, subjective.options.size)
        assertNull(service.yesAdditionalPrice(subjective))
        assertEquals(0L, service.yesAdditionalPrice(service.newOptionGroup(event(), "a", "b", OptionGroupType.TRUE_FALSE, null)))
    }

    @Test
    fun `주관식 추가금, 음수 추가금, 객관식은 400`() {
        assertThrows<InvalidOptionPriceException> { service.newOptionGroup(event(), "a", "b", OptionGroupType.SUBJECTIVE, 1000) }
        assertThrows<InvalidOptionPriceException> { service.newOptionGroup(event(), "a", "b", OptionGroupType.TRUE_FALSE, -1) }
        assertThrows<InvalidOptionPriceException> { service.newOptionGroup(event(), "a", "b", OptionGroupType.TRUE_FALSE, 10_000_001) }
        assertEquals(10_000_000L, service.yesAdditionalPrice(service.newOptionGroup(event(), "a", "b", OptionGroupType.TRUE_FALSE, 10_000_000)))
        assertThrows<UnsupportedV2OptionTypeException> { service.newOptionGroup(event(), "a", "b", OptionGroupType.MULTIPLE_CHOICE, null) }
    }

    @Test
    fun `판매된 티켓에 붙으면 잠김 - 이름·설명만, 추가금 변경 400 (같은 값은 허용)`() {
        val option = yesNo(price = 1000)
        val soldTicket = ticket(TicketPayType.DUDOONG_TICKET, option).also { it.reduceQuantity(1) }
        val tickets = listOf(soldTicket)
        assertTrue(service.isLocked(option, tickets, emptySet()))

        service.applyUpdate(option, tickets, emptySet(), name = "새이름", description = "새설명", yesAdditionalPrice = 1000)
        assertEquals("새이름", option.name)
        assertEquals("새설명", option.description)
        assertThrows<ForbiddenLockedOptionChangeException> { service.applyUpdate(option, tickets, emptySet(), null, null, 2000) }
        assertEquals(1000L, service.yesAdditionalPrice(option))
    }

    @Test
    fun `판매 전 티켓에만 붙으면 추가금 변경 가능, 무료티켓에 붙은 옵션은 0 초과 불가`() {
        val option = yesNo(price = 0)
        val paid = ticket(TicketPayType.DUDOONG_TICKET, option)
        assertFalse(service.isLocked(option, listOf(paid), emptySet()))
        service.applyUpdate(option, listOf(paid), emptySet(), null, null, 5000)
        assertEquals(5000L, service.yesAdditionalPrice(option))
        assertEquals(Money.ZERO, option.options.first { it.answer == KR_NO }.additionalPrice)

        val freeOption = yesNo(price = 0, id = 2L)
        val free = ticket(TicketPayType.FREE_TICKET, freeOption)
        assertThrows<ForbiddenOptionPriceException> { service.applyUpdate(freeOption, listOf(free), emptySet(), null, null, 1) }
        // 주관식은 0 만
        val subjective = service.newOptionGroup(event(), "a", "b", OptionGroupType.SUBJECTIVE, null)
        ReflectionTestUtils.setField(subjective, "id", 3L)
        service.applyUpdate(subjective, emptyList(), emptySet(), null, null, 0)
        assertThrows<InvalidOptionPriceException> { service.applyUpdate(subjective, emptyList(), emptySet(), null, null, 100) }
    }

    @Test
    fun `승인 대기 주문이 있는 티켓에 붙으면 재고 감소가 없어도 잠김 - 추가금 변경 400`() {
        val option = yesNo(price = 1000)
        val ticket = ticket(TicketPayType.DUDOONG_TICKET, option)
        ReflectionTestUtils.setField(ticket, "id", 99L)
        assertFalse(service.isLocked(option, listOf(ticket), emptySet()))
        assertTrue(service.isLocked(option, listOf(ticket), setOf(99L)))
        assertThrows<ForbiddenLockedOptionChangeException> { service.applyUpdate(option, listOf(ticket), setOf(99L), null, null, 2000) }
        service.applyUpdate(option, listOf(ticket), setOf(99L), "이름만", null, null)
        assertEquals("이름만", option.name)
    }
}
