package band.gosrock.domain.domains.order.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import band.gosrock.domain.domains.order.exception.CanNotCancelOrderException;
import band.gosrock.domain.domains.order.exception.CanNotRefundOrderException;
import band.gosrock.domain.domains.order.exception.NotPendingOrderException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class OrderStatusTest {


    @Test
    void 주문상태_취소가능_상태_확인(){
        OrderStatus.CONFIRM.validCanCancel();
        OrderStatus.APPROVED.validCanCancel();
    }

    @Test
    void 주문상태_취소_불가능_상태_확인(){
        //given
        List<Executable> executables = Arrays.stream(OrderStatus.values())
            .filter(orderStatus -> !canWithDrawStatus(orderStatus))
            .<Executable>map(orderStatus -> orderStatus::validCanCancel
            ).toList();
        //then
        executables.forEach(
            executable->
                assertThrows(CanNotCancelOrderException.class, executable));
    }

    @Test
    void 주문상태_환불가능_상태_확인(){
        OrderStatus.CONFIRM.validCanRefund();
        OrderStatus.APPROVED.validCanRefund();
    }

    @Test
    void 주문상태_환불_불가능_상태_확인(){
        //given
        List<Executable> executables = Arrays.stream(OrderStatus.values())
            .filter(orderStatus -> !canWithDrawStatus(orderStatus))
            .<Executable>map(orderStatus -> orderStatus::validCanRefund
            ).toList();
        //then
        executables.forEach(
            executable->
                assertThrows(CanNotRefundOrderException.class, executable));
    }

    @Test
    void 주문상태_결제방식_승인_상태_확인(){
        OrderStatus.PENDING_PAYMENT.validCanPaymentConfirm();
    }

    @Test
    void 주문상태_결제방식_승인_불가능_상태_확인(){
        //given
        List<Executable> executables = Arrays.stream(OrderStatus.values())
            .filter(orderStatus -> !orderStatus.equals(OrderStatus.PENDING_PAYMENT))
            .<Executable>map(orderStatus -> orderStatus::validCanPaymentConfirm
            ).toList();
        //then
        executables.forEach(
            executable->
                assertThrows(NotPendingOrderException.class, executable));
    }

    @Test
    void 주문상태_승인방식_승인_상태_확인(){
        OrderStatus.PENDING_APPROVE.validCanApprove();
    }

    @Test
    void 주문상태_승인방식_승인_불가능_상태_확인(){
        //given
        List<Executable> executables = Arrays.stream(OrderStatus.values())
            .filter(orderStatus -> !orderStatus.equals(OrderStatus.PENDING_APPROVE))
            .<Executable>map(orderStatus -> orderStatus::validCanApprove
            ).toList();
        //then
        executables.forEach(
            executable->
                assertThrows(NotPendingOrderException.class, executable));
    }

    private static boolean canWithDrawStatus(OrderStatus orderStatus) {
        return orderStatus.equals(OrderStatus.CONFIRM) || orderStatus.equals(OrderStatus.APPROVED);
    }
}