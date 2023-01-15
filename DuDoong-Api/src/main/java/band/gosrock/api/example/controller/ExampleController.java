package band.gosrock.api.example.controller;


import band.gosrock.api.example.docs.ExampleException2Docs;
import band.gosrock.api.example.docs.ExampleExceptionDocs;
import band.gosrock.api.example.dto.ExampleResponse;
import band.gosrock.api.example.service.ExampleApiService;
import band.gosrock.common.annotation.ApiErrorCodeExample;
import band.gosrock.common.annotation.ApiErrorExceptionsExample;
import band.gosrock.common.annotation.DevelopOnlyApi;
import band.gosrock.common.exception.GlobalErrorCode;
import band.gosrock.domain.domains.order.exception.OrderErrorCode;
import band.gosrock.domain.domains.user.exception.UserErrorCode;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsCancelErrorCode;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsConfirmErrorCode;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsCreateErrorCode;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TransactionGetErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/examples")
@RequiredArgsConstructor
@SecurityRequirement(name = "access-token")
@Tag(name = "예시 컨트롤러, 실사용 용도가 아닙니다. 에러코드들의 목록을 가지고 있는 api 도 있습니다.")
public class ExampleController {

    private final ExampleApiService exampleApiService;

    @GetMapping
    @DevelopOnlyApi
    @ApiErrorExceptionsExample(ExampleExceptionDocs.class)
    public ExampleResponse get() {
        return exampleApiService.getExample();
    }

    @PostMapping
    @ApiErrorExceptionsExample(ExampleException2Docs.class)
    public ExampleResponse create() {
        return exampleApiService.createExample();
    }

    @GetMapping("/global")
    @DevelopOnlyApi
    @Operation(summary = "글로벌 ( 인증 , aop, 서버 내부 오류등)  관련 에러 코드 나열")
    @ApiErrorCodeExample(GlobalErrorCode.class)
    public void getGlobalErrorCode() {}

    @GetMapping("/user")
    @DevelopOnlyApi
    @Operation(summary = "유저 도메인 관련 에러 코드 나열")
    @ApiErrorCodeExample(UserErrorCode.class)
    public void getUserErrorCode() {}

    @GetMapping("/order")
    @DevelopOnlyApi
    @Operation(summary = "주문 도메인 관련 에러 코드 나열")
    @ApiErrorCodeExample(OrderErrorCode.class)
    public void getOrderErrorCode() {}

    @GetMapping("/toss/create")
    @DevelopOnlyApi
    @Operation(summary = "토스 주문 생성 관련 에러 코드 나열")
    @ApiErrorCodeExample(PaymentsCreateErrorCode.class)
    public void getPaymentsCreateErrorCode() {}

    @GetMapping("/toss/confirm")
    @DevelopOnlyApi
    @Operation(summary = "토스 주문 승인 관련 에러 코드 나열")
    @ApiErrorCodeExample(PaymentsConfirmErrorCode.class)
    public void getPaymentsConfirmErrorCode() {}

    @GetMapping("/toss/cancel")
    @DevelopOnlyApi
    @Operation(summary = "토스 주문 취소 관련 에러 코드 나열")
    @ApiErrorCodeExample(PaymentsCancelErrorCode.class)
    public void getPaymentsCancelErrorCode() {}

    @GetMapping("/toss/transaction")
    @DevelopOnlyApi
    @Operation(summary = "토스 거래 조회 관련 에러 코드 나열")
    @ApiErrorCodeExample(TransactionGetErrorCode.class)
    public void getTransactionGetErrorCode() {}
}
