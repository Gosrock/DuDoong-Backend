package band.gosrock.domain.common.alarm;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public class HostSlackAlarm {

    public static String joinOf(Host host, User user) {
        return nameOf(user) + "님이 " + nameOf(host) + "에 가입했습니다!";
    }

    public static String slackRegistrationOf(Host host) {
        return nameOf(host) + "에 슬랙 알림이 등록되었습니다! 환영합니다";
    }

    public static String changeMasterOf(Host host, User user) {
        return nameOf(host) + "의 마스터가 " + nameOf(user) + "님으로 변경되었습니다.";
    }

    public static String disabledOf(User user) {
        return nameOf(user) + "님이 호스트에서 추방당했습니다.";
    }

    private static String getEventOrderTitle(Event event) {
        return event.getEventBasic().getName() + " 이벤트 주문관련 알림\n";
    }

    public static String newConfirmOrder(Event event, Order order) {
        return getEventOrderTitle(event)
                + getOrderNo(order)
                + " 주문이 결제되었습니다.\n"
                + getOrderNameAndAmount(order);
    }

    public static String newApproveOrder(Event event, Order order) {
        return getEventOrderTitle(event)
                + getOrderNo(order)
                + " 주문 승인이 요청 되었습니다.\n"
                + getOrderNameAndAmount(order);
    }

    public static String ApprovedOrder(Event event, Order order) {
        return getEventOrderTitle(event)
                + getOrderNo(order)
                + " 주문이 승인 되었습니다.\n"
                + getOrderNameAndAmount(order);
    }

    @NotNull
    private static String getOrderNo(Order order) {
        return "주문번호 : " + order.getOrderNo();
    }

    public static String withDrawOrder(Event event, Order order) {
        return getEventOrderTitle(event)
                + getOrderNo(order)
                + " 주문이 철회 되었습니다.\n"
                + getOrderNameAndAmount(order);
    }

    public static String refundRequestOrder(Event event, Order order) {
        return getEventOrderTitle(event)
                + getOrderNo(order)
                + " 두둥티켓 주문이 구매자에의해 환불 처리 되었습니다. 구매자에게 연락해서 환불을 진행해 주세요.\n"
                + getOrderNameAndAmount(order);
    }

    private static String getOrderNameAndAmount(Order order) {
        return "주문이름 :" + order.getOrderName() + " | 주문 금액 : " + order.getTotalPaymentPrice();
    }

    private static String nameOf(Host host) {
        return "'" + host.getProfile().getName() + "'";
    }

    private static String nameOf(User user) {
        return "'" + user.getProfile().getName() + "'";
    }
}
