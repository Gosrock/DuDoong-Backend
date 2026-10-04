package band.gosrock.api.v2.order.dto.request

import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import band.gosrock.domain.domains.order.service.v2.V2OrderAnswerForm
import band.gosrock.domain.domains.order.service.v2.V2RefundAccountForm
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/** O-1 주문 생성 (= 사용자가 "입금했어요"/무료 "신청"을 누른 시점) */
data class V2CreateOrderRequest(
    @field:NotNull
    val eventId: Long?,

    @field:NotNull
    @field:Schema(description = "티켓 id (1주문 1종류)")
    val ticketItemId: Long?,

    @field:NotNull
    @field:Min(1)
    @field:Max(MAX_QUANTITY)
    @field:Schema(description = "수량 (1인 매수 제한·재고는 서버가 검사)")
    val quantity: Long?,

    @field:Valid
    @field:Schema(description = "모든 티켓 동일 옵션. applyToAll=true 면 answers 를 수량 전체에 적용. 옵션이 없는 티켓은 생략 가능")
    val options: V2OrderOptionsRequest? = null,

    @field:Size(max = MAX_QUANTITY.toInt())
    @field:Schema(description = "티켓별 옵션 (options.applyToAll=false 일 때). 수량과 같은 개수의 답변 묶음, 다르면 Order_400_23")
    val perTicketOptions: List<@Valid List<@Valid V2OrderAnswerRequest>>? = null,

    @field:NotNull
    @field:Schema(description = "BANK_TRANSFER(직접 계좌이체) / TOSS_TRANSFER(토스 송금) — 두둥티켓, FREE — 무료티켓. 안 맞으면 Order_400_21")
    val paymentMethod: OrderPaymentChannel?,

    @field:Size(max = 100)
    @field:Schema(description = "입금자명 (두둥티켓 필수, 앞뒤 공백 제외 1~20자, 기본값은 프론트가 닉네임으로 채움). 무료티켓은 무시", example = "128구구")
    val depositorName: String? = null,

    @field:NotNull
    @field:AssertTrue(message = "환불 규정에 동의해야 주문할 수 있습니다.")
    @field:Schema(description = "환불/취소 규정 동의 (true 필수)")
    val agreeRefundPolicy: Boolean? = null,
) {
    /** 도메인 입력용 답변 묶음. 일괄이면 1개, 티켓별이면 perTicketOptions 그대로 */
    fun answerSets(): List<List<V2OrderAnswerForm>> =
        if (applyToAll()) listOf(options?.answers.orEmpty().map { it.toForm() })
        else perTicketOptions.orEmpty().map { set -> set.map { it.toForm() } }

    /** options 생략 = 일괄(옵션 없는 티켓) */
    fun applyToAll(): Boolean = options?.applyToAll ?: true

    companion object {
        const val MAX_QUANTITY = 100L
    }
}

data class V2OrderOptionsRequest(
    @field:NotNull
    val applyToAll: Boolean?,

    @field:Size(max = 50)
    val answers: List<@Valid V2OrderAnswerRequest>? = null,
)

data class V2OrderAnswerRequest(
    @field:NotNull
    @field:Schema(description = "옵션 id (공개 티켓 목록 P-5 의 options[].optionId)")
    val optionId: Long?,

    @field:Size(max = 300)
    @field:Schema(description = "네/아니오 옵션은 YES / NO (예·네 / 아니요·아니오 도 허용), 주관식은 1~255자", example = "YES")
    val answer: String?,
) {
    fun toForm() = V2OrderAnswerForm(optionId = optionId!!, answer = answer)
}

/** O-4 사용자 취소·환불 요청 */
data class V2CancelMyOrderRequest(
    @field:Valid
    @field:Schema(description = "환불 받을 계좌. 유료(두둥티켓) 주문은 필수(없으면 Order_400_25), 무료는 무시")
    val refundAccount: V2RefundAccountRequest? = null,
)

data class V2RefundAccountRequest(
    @field:NotBlank
    @field:Size(max = OrderRefundAccount.BANK_NAME_MAX_LENGTH)
    @field:Schema(example = "신한은행")
    val bankName: String?,

    @field:NotBlank
    @field:Size(max = OrderRefundAccount.ACCOUNT_HOLDER_MAX_LENGTH)
    @field:Schema(example = "홍길동")
    val accountHolder: String?,

    @field:NotBlank
    @field:Size(max = OrderRefundAccount.ACCOUNT_NUMBER_MAX_LENGTH)
    @field:jakarta.validation.constraints.Pattern(regexp = "^[0-9][0-9 -]*[0-9]$", message = "계좌번호는 숫자와 하이픈만 입력해 주세요.")
    @field:Schema(description = "숫자·하이픈(공백 허용)", example = "110-123-456789")
    val accountNumber: String?,
) {
    fun toForm() = V2RefundAccountForm(bankName = bankName!!, accountHolder = accountHolder!!, accountNumber = accountNumber!!)
}
