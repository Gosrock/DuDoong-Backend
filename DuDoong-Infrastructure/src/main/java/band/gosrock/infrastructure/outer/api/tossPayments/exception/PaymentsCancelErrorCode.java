package band.gosrock.infrastructure.outer.api.tossPayments.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import band.gosrock.common.annotation.ErrorCode;
import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ErrorCode
public enum PaymentsCancelErrorCode implements BaseErrorCode {
    ALREADY_CANCELED_PAYMENT(
            INTERNAL_SERVER_ERROR.value(),
            "PAYMENTS_GET_ALREADY_CANCELED_PAYMENT",
            "이미 취소된 결제 입니다."),
    INVALID_REFUND_ACCOUNT_INFO(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_INVALID_REFUND_ACCOUNT_INFO",
            "환불 계좌번호와 예금주명이 일치하지 않습니다."),
    EXCEED_CANCEL_AMOUNT_DISCOUNT_AMOUNT(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_EXCEED_CANCEL_AMOUNT_DISCOUNT_AMOUNT",
            "즉시할인금액보다 적은 금액은 부분취소가 불가능합니다."),
    INVALID_REQUEST(BAD_REQUEST.value(), "PAYMENTS_GET_NOT_FOUND", "잘못된 요청입니다."),
    INVALID_REFUND_ACCOUNT_NUMBER(
            BAD_REQUEST.value(), "PAYMENTS_GET_INVALID_REFUND_ACCOUNT_NUMBER", "잘못된 환불 계좌번호입니다."),
    INVALID_BANK(BAD_REQUEST.value(), "PAYMENTS_GET_INVALID_BANK", "잘못된 요청입니다."),
    NOT_MATCHES_REFUNDABLE_AMOUNT(
            BAD_REQUEST.value(), "PAYMENTS_GET_NOT_MATCHES_REFUNDABLE_AMOUNT", "잔액 결과가 일치하지 않습니다."),
    PROVIDER_ERROR(
            BAD_REQUEST.value(), "PAYMENTS_GET_PROVIDER_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    UNAUTHORIZED_KEY(
            BAD_REQUEST.value(), "PAYMENTS_GET_UNAUTHORIZED_KEY", "인증되지 않은 시크릿 키 혹은 클라이언트 키 입니다."),
    NOT_CANCELABLE_AMOUNT(
            BAD_REQUEST.value(), "PAYMENTS_GET_NOT_CANCELABLE_AMOUNT", "취소 할 수 없는 금액 입니다."),
    FORBIDDEN_CONSECUTIVE_REQUEST(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_FORBIDDEN_CONSECUTIVE_REQUEST",
            "반복적인 요청은 허용되지 않습니다. 잠시 후 다시 시도해주세요."),
    FORBIDDEN_REQUEST(BAD_REQUEST.value(), "PAYMENTS_GET_FORBIDDEN_REQUEST", "허용되지 않은 요청입니다."),
    NOT_CANCELABLE_PAYMENT(
            BAD_REQUEST.value(), "PAYMENTS_GET_NOT_CANCELABLE_PAYMENT", "취소 할 수 없는 결제 입니다."),
    EXCEED_MAX_REFUND_DUE(
            BAD_REQUEST.value(), "PAYMENTS_GET_EXCEED_MAX_REFUND_DUE", "환불 가능한 기간이 지났습니다."),
    NOT_ALLOWED_PARTIAL_REFUND_WAITING_DEPOSIT(
            BAD_REQUEST.value(), "PAYMENTS_GET_NOT_FOUND", "입금 대기중인 결제는 부분 환불이 불가합니다"),
    NOT_ALLOWED_PARTIAL_REFUND(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_NOT_ALLOWED_PARTIAL_REFUND",
            "에스크로 주문이나 체크 카드 결제는 부분 환불이 되지 않습니다."),
    NOT_AVAILABLE_BANK(BAD_REQUEST.value(), "PAYMENTS_GET_NOT_AVAILABLE_BANK", "은행 서비스 시간이 아닙니다."),
    NOT_FOUND_PAYMENT(BAD_REQUEST.value(), "PAYMENTS_GET_NOT_FOUND_PAYMENT", "존재하지 않는 결제 정보 입니다."),
    FAILED_INTERNAL_SYSTEM_PROCESSING(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_FAILED_INTERNAL_SYSTEM_PROCESSING",
            "내부 시스템 처리 작업이 실패했습니다. 잠시 후 다시 시도해주세요."),
    FAILED_REFUND_PROCESS(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_FAILED_REFUND_PROCESS",
            "은행 응답시간 지연이나 일시적인 오류로 환불요청에 실패했습니다."),
    FAILED_METHOD_HANDLING_CANCEL(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_FAILED_METHOD_HANDLING_CANCEL",
            "취소 중 결제 시 사용한 결제 수단 처리과정에서 일시적인 오류가 발생했습니다."),
    FAILED_PARTIAL_REFUND(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_FAILED_PARTIAL_REFUND",
            "은행 점검, 해약 계좌 등의 사유로 부분 환불이 실패했습니다."),
    COMMON_ERROR(
            BAD_REQUEST.value(), "PAYMENTS_GET_COMMON_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING(
            BAD_REQUEST.value(),
            "PAYMENTS_GET_FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING",
            "결제가 완료되지 않았어요. 다시 시도해주세요.");
    private int status;
    private String code;
    private String reason;

    @Override
    public ErrorReason getErrorReason() {
        return ErrorReason.builder().status(status).code(code).reason(reason).build();
    }
}
