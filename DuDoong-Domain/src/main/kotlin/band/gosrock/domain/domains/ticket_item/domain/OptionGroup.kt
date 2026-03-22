package band.gosrock.domain.domains.ticket_item.domain

import band.gosrock.common.consts.DuDoongStatic.KR_NO
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType.SUBJECTIVE
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType.TRUE_FALSE
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionGroupDeleteException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionGroupException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionPriceException
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault

@Entity(name = "tbl_option_group")
class OptionGroup(
    var eventId: Long? = null,
    // 옵션 그룹 응답 형식
    @Enumerated(EnumType.STRING)
    var type: OptionGroupType? = null,
    // 옵션 그룹 이름
    var name: String? = null,
    // 옵션 그룹 설명
    var description: String? = null,
    // 필수 응답 여부
    var isEssential: Boolean? = null,
    initialOptions: List<Option> = emptyList(),
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_group_id")
    var id: Long? = null
        protected set

    // 상태
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'VALID'")
    var optionGroupStatus: OptionGroupStatus = OptionGroupStatus.VALID
        protected set

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "optionGroup")
    val options: MutableList<Option> = mutableListOf()

    init {
        if (initialOptions.isNotEmpty()) {
            this.options.addAll(initialOptions)
            initialOptions.forEach { it.updateOptionGroup(this) }
        }
    }

    fun validateEventId(eventId: Long) {
        if (this.eventId != eventId) throw InvalidOptionGroupException.EXCEPTION
    }

    fun hasApplication(ticketItems: List<TicketItem>): Boolean =
        ticketItems.any { it.hasItemOptionGroup(this.id!!) }

    fun createTicketOption(additionalPrice: Money): OptionGroup {
        when (type) {
            TRUE_FALSE -> {
                if (additionalPrice.isLessThan(Money.ZERO)) throw InvalidOptionPriceException.EXCEPTION
                this.options.add(Option.create(KR_YES, additionalPrice, this))
                this.options.add(Option.create(KR_NO, Money.ZERO, this))
            }
            SUBJECTIVE -> {
                this.options.add(Option.create("", Money.ZERO, this))
            }
            else -> {}
        }
        return this
    }

    fun softDeleteOptionGroup(ticketItems: List<TicketItem>) {
        // 적용된 옵션은 삭제 불가
        if (this.hasApplication(ticketItems)) throw ForbiddenOptionGroupDeleteException.EXCEPTION
        this.optionGroupStatus = OptionGroupStatus.DELETED
    }
}
