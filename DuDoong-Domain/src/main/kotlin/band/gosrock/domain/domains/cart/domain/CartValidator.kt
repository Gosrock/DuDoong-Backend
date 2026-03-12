package band.gosrock.domain.domains.cart.domain

import band.gosrock.common.annotation.Validator
import band.gosrock.domain.domains.cart.exception.CartItemNotOneTypeException
import band.gosrock.domain.domains.cart.exception.CartNotAnswerAllOptionGroupException
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.Option
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import java.util.Objects

@Validator
class CartValidator(
    private val itemAdaptor: TicketItemAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val optionAdaptor: OptionAdaptor,
) {
    fun validCanCreate(cart: Cart) {
        validItemKindIsOneType(cart)
        validCorrectAnswer(cart)
        val item = getItem(cart)
        val event = eventAdaptor.findById(item.eventId!!)
        validAnswerToAllQuestion(cart, item)
        validEventIsOpen(event)
        validTicketingTime(event)
        validItemStockEnough(cart, item)
        validItemPurchaseLimit(cart, item)
    }

    fun validItemPurchaseLimit(cart: Cart, item: TicketItem) {
        val paidTicketCount = issuedTicketAdaptor.countPaidTicket(cart.userId!!, item.id!!)
        val totalIssuedCount = paidTicketCount + cart.getTotalQuantity()
        item.validPurchaseLimit(totalIssuedCount)
    }

    fun validTicketingTime(event: Event) {
        event.validateTicketingTime()
    }

    fun validItemStockEnough(cart: Cart, item: TicketItem) {
        item.validEnoughQuantity(cart.getTotalQuantity())
    }

    fun validEventIsOpen(event: Event) {
        event.validateNotOpenStatus()
    }

    fun validItemKindIsOneType(cart: Cart) {
        val itemIds = cart.getDistinctItemIds()
        if (itemIds.size != 1) {
            throw CartItemNotOneTypeException.EXCEPTION
        }
    }

    fun validAnswerToAllQuestion(cart: Cart, item: TicketItem) {
        val cartLineItems = cart.cartLineItems
        val itemsOptionGroupIds = item.getOptionGroupIds()
        cartLineItems.forEach { cartLineItem ->
            if (!Objects.equals(getAnswerOptionGroupIds(cartLineItem), itemsOptionGroupIds)) {
                throw CartNotAnswerAllOptionGroupException.EXCEPTION
            }
        }
    }

    fun validCorrectAnswer(cart: Cart) {
        val cartLineItems = cart.cartLineItems
        cartLineItems.forEach { cartLineItem ->
            val cartOptionAnswers = cartLineItem.cartOptionAnswers
            val options = getOptionsFrom(cartLineItem)
            cartOptionAnswers.forEach { cartOptionAnswer ->
                val optionId = cartOptionAnswer.optionId!!
                findOptionFromCartOptionAnswer(options, optionId)
                    .validCorrectAnswer(cartOptionAnswer.answer ?: "")
            }
        }
    }

    private fun getOptionsFrom(cartLineItem: CartLineItem): List<Option> =
        optionAdaptor.findAllByIds(cartLineItem.getAnswerOptionIds())

    private fun findOptionFromCartOptionAnswer(options: List<Option>, optionId: Long): Option =
        options.first { it.id == optionId }

    private fun getAnswerOptionGroupIds(cartLineItem: CartLineItem): List<Long> {
        val answerOptions = getOptionsFrom(cartLineItem)
        return answerOptions.map { it.getOptionGroupId()!! }.sorted()
    }

    private fun getItem(cart: Cart): TicketItem =
        itemAdaptor.queryTicketItem(cart.getItemId())
}
