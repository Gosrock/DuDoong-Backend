package band.gosrock.domain.domains.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderLineItemTest {

    @Mock
    OptionAnswerVo optionAnswerVo;

    @Mock
    TicketItem ticketItem;
}