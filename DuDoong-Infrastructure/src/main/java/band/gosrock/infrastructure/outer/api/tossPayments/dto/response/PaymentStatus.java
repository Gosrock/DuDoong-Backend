package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {
    // 결제 처리 상태
    // - READY: 결제를 생성하면 가지게 되는 초기 상태 입니다. 인증 전까지는 READY 상태를 유지합니다.
    //
    // - IN_PROGRESS: 결제 수단 정보와 해당 결제 수단의 소유자가 맞는지 인증을 마친 상태입니다. 결제 승인 API를 호출하면 결제가 완료됩니다.
    //
    // - WAITING_FOR_DEPOSIT: 가상계좌 결제 흐름에만 있는 상태로, 결제 고객이 발급된 가상계좌에 입금하는 것을 기다리고 있는 상태입니다.
    //
    // - DONE: 인증된 결제 수단 정보, 고객 정보로 요청한 결제가 승인된 상태입니다.
    //
    // - CANCELED: 승인된 결제가 취소된 상태입니다.
    //
    // - PARTIAL_CANCELED: 승인된 결제가 부분 취소된 상태입니다.
    //
    // - ABORTED: 결제 승인이 실패했거나, 결제 고객이 창을 닫아서 결제를 취소한 상태입니다.
    //
    // - EXPIRED: 결제 유효 시간 30분이 지나 거래가 취소된 상태입니다. IN_PROGRESS 상태에서 결제 승인 API를 호출하지 않으면 EXPIRED가 됩니다
    READY("READY"),
    IN_PROGRESS("IN_PROGRESS"),
    WAITING_FOR_DEPOSIT("WAITING_FOR_DEPOSIT"),
    DONE("DONE"),
    CANCELED("CANCELED"),
    PARTIAL_CANCELED("PARTIAL_CANCELED"),
    ABORTED("ABORTED"),
    EXPIRED("EXPIRED");

    private String value;
}
