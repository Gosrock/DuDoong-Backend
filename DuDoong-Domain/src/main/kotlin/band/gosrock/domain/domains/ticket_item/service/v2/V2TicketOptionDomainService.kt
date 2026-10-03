package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenLockedOptionChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionGroupDeleteException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.OptionGroupNotFoundException
import band.gosrock.domain.domains.ticket_item.exception.UnsupportedV2OptionTypeException
import org.springframework.transaction.annotation.Transactional

/**
 * v2 전용 옵션 규칙 (DEC-018, DEC-012). 옵션 = v1 OptionGroup (YES_NO = TRUE_FALSE, 행은 v1 과 같이 '예'/'아니오' 2개, 주관식 1개).
 * 옵션 행(tbl_option)은 주문 응답이 id 로 참조하므로 지우거나 바꿔 끼우지 않는다. 그래서 응답 형식은 만든 뒤 바꿀 수 없다.
 */
@DomainService
@Transactional(readOnly = true)
class V2TicketOptionDomainService(
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val v2EventDomainService: V2EventDomainService,
) {

    /** 이 공연의 유효 옵션 조회. 다른 공연 옵션 id 는 404 */
    fun queryOptionGroup(eventId: Long, optionGroupId: Long): OptionGroup {
        val optionGroup = optionGroupAdaptor.queryOptionGroup(optionGroupId)
        if (optionGroup.eventId != eventId) throw OptionGroupNotFoundException.EXCEPTION
        return optionGroup
    }

    /** 이 옵션이 붙은 유효 티켓 */
    fun appliedTicketItems(optionGroup: OptionGroup, ticketItems: List<TicketItem>): List<TicketItem> =
        ticketItems.filter { it.hasItemOptionGroup(optionGroup.id!!) }

    /** 판매된(재고 감소) 티켓에 붙어 있으면 잠김: 이름·설명만 수정, 삭제 불가 */
    fun isLocked(optionGroup: OptionGroup, ticketItems: List<TicketItem>): Boolean =
        appliedTicketItems(optionGroup, ticketItems).any { it.isSold() }

    /** '예' 선택 시 추가 금액. 네/아니오가 아니면 null */
    fun yesAdditionalPrice(optionGroup: OptionGroup): Long? =
        if (optionGroup.type == OptionGroupType.TRUE_FALSE) {
            (optionGroup.options.firstOrNull { it.answer == KR_YES }?.additionalPrice ?: Money.ZERO).longValue()
        } else null

    /** 생성 (저장 전). 주관식은 추가 금액 없음, 네/아니오는 0 이상 (없으면 0) */
    fun newOptionGroup(event: Event, name: String, description: String, type: OptionGroupType, yesAdditionalPrice: Long?): OptionGroup {
        v2EventDomainService.validateEditable(event)
        val price = validatePrice(type, yesAdditionalPrice)
        return OptionGroup(
            eventId = event.id,
            type = type,
            name = name.trim(),
            description = description.trim(),
            isEssential = true,
        ).createTicketOption(Money.wons(price))
    }

    @Transactional
    fun createOptionGroup(event: Event, name: String, description: String, type: OptionGroupType, yesAdditionalPrice: Long?): OptionGroup =
        optionGroupAdaptor.save(newOptionGroup(event, name, description, type, yesAdditionalPrice))

    /**
     * 부분 수정 (null 은 변경 안 함). 응답 형식은 바꿀 수 없다.
     * 잠긴 옵션(판매된 티켓에 붙음)은 이름·설명만, 추가 금액을 다른 값으로 바꾸면 400 (DEC-012).
     * 무료티켓에 붙은 옵션은 추가 금액을 0 보다 크게 할 수 없다 (v1 옵션 적용 규칙과 같음)
     */
    fun applyUpdate(optionGroup: OptionGroup, ticketItems: List<TicketItem>, name: String?, description: String?, yesAdditionalPrice: Long?) {
        if (yesAdditionalPrice != null) {
            val price = validatePrice(optionGroup.type!!, yesAdditionalPrice)
            // 주관식은 0 만 허용(위에서 검증)되고 바꿀 금액이 없다
            if (optionGroup.type == OptionGroupType.TRUE_FALSE && price != yesAdditionalPrice(optionGroup)) {
                val applied = appliedTicketItems(optionGroup, ticketItems)
                if (applied.any { it.isSold() }) throw ForbiddenLockedOptionChangeException.EXCEPTION
                if (price > 0 && applied.any { it.payType == TicketPayType.FREE_TICKET }) throw ForbiddenOptionPriceException.EXCEPTION
                optionGroup.options.first { it.answer == KR_YES }.additionalPrice = Money.wons(price)
            }
        }
        if (name != null) optionGroup.name = name.trim()
        if (description != null) optionGroup.description = description.trim()
    }

    @Transactional
    fun updateOptionGroup(eventId: Long, optionGroupId: Long, name: String?, description: String?, yesAdditionalPrice: Long?) {
        v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
        val optionGroup = queryOptionGroup(eventId, optionGroupId)
        applyUpdate(optionGroup, ticketItemAdaptor.findAllByEventId(eventId), name, description, yesAdditionalPrice)
        optionGroupAdaptor.save(optionGroup)
    }

    /** 삭제: 판매된 티켓에 붙어 있으면 400. 판매 전 티켓에 붙어 있으면 떼어 내고 삭제한다 */
    @Transactional
    fun deleteOptionGroup(eventId: Long, optionGroupId: Long) {
        v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
        val optionGroup = queryOptionGroup(eventId, optionGroupId)
        val ticketItems = ticketItemAdaptor.findAllByEventId(eventId)
        val applied = appliedTicketItems(optionGroup, ticketItems)
        if (applied.any { it.isSold() }) throw ForbiddenOptionGroupDeleteException.EXCEPTION
        applied.forEach {
            it.removeItemOptionGroup(optionGroup)
            ticketItemAdaptor.save(it)
        }
        optionGroup.softDeleteOptionGroup(ticketItems)
        optionGroupAdaptor.save(optionGroup)
    }

    private fun validatePrice(type: OptionGroupType, yesAdditionalPrice: Long?): Long = when (type) {
        OptionGroupType.TRUE_FALSE -> {
            val price = yesAdditionalPrice ?: 0L
            if (price < 0) throw InvalidOptionPriceException.EXCEPTION
            price
        }
        OptionGroupType.SUBJECTIVE -> {
            if (yesAdditionalPrice != null && yesAdditionalPrice != 0L) throw InvalidOptionPriceException.EXCEPTION
            0L
        }
        OptionGroupType.MULTIPLE_CHOICE -> throw UnsupportedV2OptionTypeException.EXCEPTION
    }

    companion object {
        const val NAME_MAX_LENGTH = 20
        const val DESCRIPTION_MAX_LENGTH = 255
    }
}
