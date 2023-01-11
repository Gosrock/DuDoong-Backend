package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CardAcquireStatus {
    //- READY: 아직 매입 요청이 안 된 상태입니다.
    //
    //- REQUESTED: 매입이 요청된 상태입니다.
    //
    //- COMPLETED: 요청된 매입이 완료된 상태입니다.
    //
    //- CANCEL_REQUESTED: 매입 취소가 요청된 상태입니다.
    //
    //- CANCELED: 요청된 매입 취소가 완료된 상태입니다.
    READY("READY"),
    REQUESTED("REQUESTED"),
    COMPLETED("COMPLETED"),
    CANCEL_REQUESTED("CANCEL_REQUESTED"),
    CANCELED("CANCELED");

    private String value;
}
