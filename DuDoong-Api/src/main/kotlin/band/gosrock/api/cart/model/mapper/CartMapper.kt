package band.gosrock.api.cart.model.mapper

import band.gosrock.api.cart.model.dto.request.AddCartLineDto
import band.gosrock.api.cart.model.dto.request.AddCartOptionAnswerDto
import band.gosrock.api.cart.model.dto.request.AddCartRequest
import band.gosrock.api.cart.model.dto.response.CartItemResponse
import band.gosrock.api.cart.model.dto.response.CartResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import band.gosrock.domain.domains.cart.domain.Cart
import band.gosrock.domain.domains.cart.domain.CartLineItem
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer
import band.gosrock.domain.domains.cart.domain.CartValidator
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.Option
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import org.springframework.transaction.annotation.Transactional

@Mapper
class CartMapper(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val optionAdaptor: OptionAdaptor,
    private val cartValidator: CartValidator,
    private val eventAdaptor: EventAdaptor,
    private val cartAdaptor: CartAdaptor,
) {
    @Transactional(readOnly = true)
    fun toCartResponse(cartId: Long): CartResponse {
        val cart = cartAdaptor.queryCart(cartId)
        return getCartResponse(cart)
    }

    @Transactional(readOnly = true)
    fun toCartResponse(cart: Cart): CartResponse {
        return getCartResponse(cart)
    }

    private fun getCartResponse(cart: Cart): CartResponse {
        val cartLineItems = cart.cartLineItems
        val ticketItem = ticketItemAdaptor.queryTicketItem(cart.getItemId())
        val event = eventAdaptor.findById(ticketItem.eventId!!)
        val cartItemResponses = getCartItemResponses(cartLineItems, ticketItem.name!!)
        return CartResponse.of(cartItemResponses, cart, ticketItem, event)
    }

    private fun getCartItemResponses(cartLineItems: List<CartLineItem>, name: String): List<CartItemResponse> {
        return cartLineItems.map { getCartItemResponse(it, name) }
    }

    private fun getCartItemResponse(cartLineItem: CartLineItem, itemName: String): CartItemResponse {
        return CartItemResponse.of(
            cartLineItem,
            generateCartLineName(itemName, cartLineItem.quantity!!),
            getOptionAnswerVos(cartLineItem),
        )
    }

    private fun getOptionAnswerVos(cartLineItem: CartLineItem): List<OptionAnswerVo> {
        val cartOptionAnswers = cartLineItem.cartOptionAnswers
        val options = optionAdaptor.findAllByIds(getOptionIds(cartOptionAnswers))
        return cartOptionAnswers.map { cartOptionAnswer ->
            cartOptionAnswer.getOptionAnswerVo(findOption(options, cartOptionAnswer))
        }
    }

    private fun getOptionIds(cartOptionAnswers: List<CartOptionAnswer>): List<Long> {
        return cartOptionAnswers.map { it.optionId!! }
    }

    private fun findOption(options: List<Option>, cartOptionAnswer: CartOptionAnswer): Option {
        return options.first { it.id == cartOptionAnswer.optionId!! }
    }

    fun toEntity(addCartRequest: AddCartRequest, currentUserId: Long): Cart {
        val addCartLineDtos = addCartRequest.items
        val itemName = getItemName(addCartLineDtos)
        val cartLineItems = addCartLineDtos.map { addCartLineDto ->
            CartLineItem.of(
                item = getTicketItem(addCartLineDto),
                quantity = addCartLineDto.quantity,
                cartOptionAnswers = getCartOptionAnswers(addCartLineDto),
            )
        }
        return Cart.of(cartLineItems, itemName, currentUserId, cartValidator)
    }

    private fun getItemName(addCartLineDtos: List<AddCartLineDto>): String {
        val itemId = addCartLineDtos.first().itemId
        return ticketItemAdaptor.queryTicketItem(itemId).name!!
    }

    private fun getTicketItem(addCartLineDto: AddCartLineDto): TicketItem {
        return ticketItemAdaptor.queryTicketItem(addCartLineDto.itemId)
    }

    private fun generateCartLineName(itemName: String, quantity: Long): String {
        return "$itemName ${quantity}매"
    }

    private fun getCartOptionAnswers(addCartLineDto: AddCartLineDto): List<CartOptionAnswer> {
        return addCartLineDto.options.map { getCartOptionAnswer(it) }
    }

    private fun getCartOptionAnswer(addCartOptionAnswerDto: AddCartOptionAnswerDto): CartOptionAnswer {
        return CartOptionAnswer.of(
            option = optionAdaptor.queryOption(addCartOptionAnswerDto.optionId),
            answer = addCartOptionAnswerDto.answer ?: "",
        )
    }
}
