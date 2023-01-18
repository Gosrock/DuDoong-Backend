package band.gosrock.domain.domains.cart.repository;

import static band.gosrock.domain.domains.cart.domain.QCart.cart;
import static band.gosrock.domain.domains.cart.domain.QCartLineItem.cartLineItem;
import static band.gosrock.domain.domains.cart.domain.QCartOptionAnswer.cartOptionAnswer;
import static band.gosrock.domain.domains.ticket_item.domain.QItemOptionGroup.itemOptionGroup;
import static band.gosrock.domain.domains.ticket_item.domain.QOption.option;
import static band.gosrock.domain.domains.ticket_item.domain.QTicketItem.ticketItem;

import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.domain.QCart;
import band.gosrock.domain.domains.cart.domain.QCartLineItem;
import band.gosrock.domain.domains.cart.domain.QCartOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketCustomRepository;
import band.gosrock.domain.domains.ticket_item.domain.QItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.QOption;
import band.gosrock.domain.domains.ticket_item.domain.QTicketItem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CartCustomRepositoryImpl implements CartCustomRepository{


    private final JPAQueryFactory queryFactory;

    public Optional<Cart> find(Long cartId){
        Cart findCart = queryFactory.selectFrom(cart)
            .leftJoin(cart.cartLineItems ,cartLineItem)
            .fetchJoin()
            .leftJoin(cartLineItem.ticketItem , ticketItem)
            .fetchJoin()
//            .leftJoin(ticketItem.itemOptionGroups ,itemOptionGroup)
//            .fetchJoin()
//            .leftJoin(itemOptionGroup.optionGroup)
//            .fetchJoin()
//            .leftJoin(cartLineItem.cartOptionAnswers , cartOptionAnswer)
//            .fetchJoin()
//            .leftJoin(cartOptionAnswer.option , option)
//            .fetchJoin()
            .where(cart.id.eq(cartId))
            .fetchOne();
        return Optional.ofNullable(findCart);
    }

}
