package band.gosrock.domain.domains.gift.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST
import band.gosrock.common.consts.DuDoongStatic.NOT_FOUND
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import java.lang.reflect.Field

/** 티켓 선물 (#719, 11 문서 8-2 에러 코드) */
enum class TicketGiftErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    @ExplainError("선물·토큰이 없거나 내 선물이 아님 (존재를 드러내지 않는다)")
    GIFT_NOT_FOUND(NOT_FOUND, "Gift_404_1", "선물을 찾을 수 없습니다."),

    @ExplainError("선물할 수 없는 티켓: 입장함·취소됨, 공연 시작 후·공연이 OPEN 아님, 원 주문이 승인/확정 아님(취소·환불 진행 포함), 받은 티켓(재선물 불가)")
    GIFT_NOT_GIFTABLE(BAD_REQUEST, "Gift_400_1", "선물할 수 없는 티켓입니다."),

    @ExplainError("이 티켓에 대기 중인 선물이 이미 있음")
    GIFT_ALREADY_PENDING(BAD_REQUEST, "Gift_400_2", "이미 선물 중인 티켓입니다."),

    @ExplainError("대기 중이 아닌 선물 (이미 수락·거절·취소·반환됨). 수락·거절·회수·메모 수정 공통")
    GIFT_NOT_PENDING(BAD_REQUEST, "Gift_400_3", "이미 처리된 선물입니다."),

    @ExplainError("보낸 사람이 자기 링크를 수락·거절하려 함")
    GIFT_OWN_LINK(BAD_REQUEST, "Gift_400_4", "내가 보낸 선물은 받을 수 없습니다."),

    @ExplainError("선물 만료 — 공연이 끝나(종료 시각 경과 또는 정산중·지난공연·삭제) 수락·거절할 수 없음 (DEC-026 #7). 보낸 사람 회수는 가능")
    GIFT_EXPIRED(BAD_REQUEST, "Gift_400_5", "공연이 끝나 받을 수 없는 선물입니다."),

    @ExplainError("원 주문이 정상(승인/확정) 상태가 아니거나 티켓이 유효하지 않음 (수락·거절 시). 방어용 — 주문 취소 연쇄 처리로 대기 선물이 먼저 취소된다")
    GIFT_ORDER_INVALID(BAD_REQUEST, "Gift_400_6", "선물을 받을 수 없는 상태입니다."),

    @ExplainError("반환할 수 없음: 입장함·취소됨, 공연 시작 후, 보낸 사람이 탈퇴·정지됨")
    GIFT_CANNOT_RETURN(BAD_REQUEST, "Gift_400_7", "반환할 수 없는 티켓입니다."),

    @ExplainError("선물받은 티켓이 아니라 반환할 수 없음 (내 티켓이지만 받은 티켓이 아님)")
    GIFT_NOT_RECEIVED_TICKET(BAD_REQUEST, "Gift_400_8", "선물받은 티켓이 아닙니다."),

    @ExplainError("메모가 50자를 넘음 (앞뒤 공백 제외)")
    GIFT_INVALID_MEMO(BAD_REQUEST, "Gift_400_9", "메모는 50자까지 입력할 수 있습니다.");

    override fun getErrorReason(): ErrorReason =
        ErrorReason(status = status, code = code, reason = reason)

    override fun getExplainError(): String {
        val field: Field = this.javaClass.getField(this.name)
        val annotation: ExplainError? = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: this.reason
    }
}
