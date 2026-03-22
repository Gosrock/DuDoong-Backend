package band.gosrock.domain.domains.cart.service

import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import java.lang.reflect.Constructor

@ExtendWith(MockitoExtension::class)
class DoneOrderEventHandlerTest {

    @Mock
    lateinit var cartAdaptor: CartAdaptor

    @InjectMocks
    lateinit var handler: DoneOrderEventHandler

    companion object {
        private fun createDoneOrderEvent(userId: Long): DoneOrderEvent {
            val constructor: Constructor<DoneOrderEvent> =
                DoneOrderEvent::class.java.getDeclaredConstructor(
                    String::class.java, Long::class.java,
                    band.gosrock.domain.domains.order.domain.OrderMethod::class.java,
                    String::class.java, Long::class.java
                )
            constructor.isAccessible = true
            return constructor.newInstance(
                "test-order-uuid", userId,
                band.gosrock.domain.domains.order.domain.OrderMethod.APPROVAL, null, 100L
            )
        }
    }

    @Test
    fun `주문 완료 이벤트 시 해당 유저의 장바구니를 삭제한다`() {
        // given
        val event = createDoneOrderEvent(1L)

        // when
        handler.handleDoneOrderEvent(event)

        // then
        then(cartAdaptor).should(times(1)).deleteByUserId(1L)
    }

    @Test
    fun `주문 완료 이벤트가 여러 번 발생하면 그만큼 장바구니를 삭제한다`() {
        // given
        val event1 = createDoneOrderEvent(1L)
        val event2 = createDoneOrderEvent(1L)

        // when
        handler.handleDoneOrderEvent(event1)
        handler.handleDoneOrderEvent(event2)

        // then
        then(cartAdaptor).should(times(2)).deleteByUserId(1L)
    }
}
