package band.gosrock.domain.domains.settlement.domain

import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.FeeCode
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementFeeDto
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class SettlementFeeVo protected constructor() {

    @Enumerated(EnumType.STRING)
    var type: FeeCode? = null
        protected set

    @Column(name = "fee")
    var fee: Long? = null
        protected set

    constructor(type: FeeCode, fee: Long) : this() {
        this.type = type
        this.fee = fee
    }

    companion object {
        @JvmStatic
        fun from(settlementFeeDto: SettlementFeeDto): SettlementFeeVo =
            SettlementFeeVo(
                type = settlementFeeDto.type!!,
                fee = settlementFeeDto.fee!!,
            )
    }
}
