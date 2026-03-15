package band.gosrock.domain.common.alarm

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.user.domain.User

object HostSlackAlarm {
    @JvmStatic
    fun joinOf(host: Host, user: User): String = "${nameOf(user)}님이 ${nameOf(host)}에 가입했습니다!"

    @JvmStatic
    fun slackRegistrationOf(host: Host): String = "${nameOf(host)}에 슬랙 알림이 등록되었습니다! 환영합니다"

    @JvmStatic
    fun changeMasterOf(host: Host, user: User): String =
        "${nameOf(host)}의 마스터가 ${nameOf(user)}님으로 변경되었습니다."

    @JvmStatic
    fun disabledOf(user: User): String = "${nameOf(user)}님이 호스트에서 추방당했습니다."

    @JvmStatic
    fun newConfirmOrder(event: Event, order: Order): String =
        getEventOrderTitle(event) + getOrderNo(order) + " 주문이 결제되었습니다.\n" + getOrderNameAndAmount(order)

    @JvmStatic
    fun newApproveOrder(event: Event, order: Order): String =
        getEventOrderTitle(event) + getOrderNo(order) + " 주문 승인이 요청 되었습니다.\n" + getOrderNameAndAmount(order)

    @JvmStatic
    fun approvedOrder(event: Event, order: Order): String =
        getEventOrderTitle(event) + getOrderNo(order) + " 주문이 승인 되었습니다.\n" + getOrderNameAndAmount(order)

    @JvmStatic
    fun withDrawOrder(event: Event, order: Order): String =
        getEventOrderTitle(event) + getOrderNo(order) + " 주문이 철회 되었습니다.\n" + getOrderNameAndAmount(order)

    @JvmStatic
    fun dudoongOrderRefund(event: Event, order: Order): String =
        getEventOrderTitle(event) +
            getOrderNo(order) +
            " 두둥티켓 주문이 구매자에의해 환불 처리 되었습니다. 구매자에게 연락해서 환불을 진행해 주세요.\n" +
            getOrderNameAndAmount(order)

    @JvmStatic
    fun dudoongOrderCancel(event: Event, order: Order): String =
        getEventOrderTitle(event) +
            getOrderNo(order) +
            " 두둥티켓 주문이 관리자에의해 환불 처리 되었습니다. 구매자에게 연락해서 환불을 진행해 주세요.\n" +
            getOrderNameAndAmount(order)

    private fun getEventOrderTitle(event: Event): String =
        "${event.eventBasic?.name} 이벤트 주문관련 알림\n"

    private fun getOrderNo(order: Order): String = "주문번호 : ${order.orderNo}"

    private fun getOrderNameAndAmount(order: Order): String =
        "주문이름 :${order.orderName} | 주문 금액 : ${order.getTotalPaymentPrice()}"

    private fun nameOf(host: Host): String = "'${host.profile?.name}'"

    private fun nameOf(user: User): String = "'${user.profile?.name}'"
}
