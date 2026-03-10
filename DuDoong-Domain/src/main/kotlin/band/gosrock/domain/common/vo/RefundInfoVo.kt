package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime

/** 상품 취소 가능 여부를 반환합니다. */
data class RefundInfoVo(
    @DateFormat val startAt: LocalDateTime,
    val availAble: Boolean
) {
    companion object {
        @JvmStatic
        fun from(startAt: LocalDateTime): RefundInfoVo {
            val before = nowIsBefore(startAt)
            return RefundInfoVo(startAt = startAt, availAble = before)
        }

        @JvmStatic
        fun of(startAt: LocalDateTime, orderStatus: OrderStatus): RefundInfoVo {
            val availAble = if (nowIsBefore(startAt)) orderStatus.isCanWithDraw else false
            return RefundInfoVo(startAt = startAt, availAble = availAble)
        }

        private fun nowIsBefore(startAt: LocalDateTime): Boolean =
            LocalDateTime.now().isBefore(startAt)
    }
}
