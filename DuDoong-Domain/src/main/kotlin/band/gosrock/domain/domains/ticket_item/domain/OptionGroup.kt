package band.gosrock.domain.domains.ticket_item.domain

import band.gosrock.common.consts.DuDoongStatic.KR_NO
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType.SUBJECTIVE
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType.TRUE_FALSE
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionGroupDeleteException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionGroupException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionPriceException
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault

@Entity(name = "tbl_option_group")
class OptionGroup() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_group_id")
    var id: Long? = null
        protected set

    var eventId: Long? = null
        protected set

    // 옵션 그룹 응답 형식
    @Enumerated(EnumType.STRING)
    var type: OptionGroupType? = null
        protected set

    // 옵션 그룹 이름
    var name: String? = null
        protected set

    // 옵션 그룹 설명
    var description: String? = null
        protected set

    // 필수 응답 여부
    var isEssential: Boolean? = null
        protected set

    // 상태
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'VALID'")
    var optionGroupStatus: OptionGroupStatus = OptionGroupStatus.VALID
        protected set

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "optionGroup")
    val options: MutableList<Option> = mutableListOf()

    constructor(
        eventId: Long?,
        type: OptionGroupType?,
        name: String?,
        description: String?,
        isEssential: Boolean?,
        options: List<Option>,
    ) : this() {
        this.eventId = eventId
        this.type = type
        this.name = name
        this.description = description
        this.isEssential = isEssential
        this.options.addAll(options)
        options.forEach { it.updateOptionGroup(this) }
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

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var eventId: Long? = null
        private var type: OptionGroupType? = null
        private var name: String? = null
        private var description: String? = null
        private var isEssential: Boolean? = null
        private var options: List<Option> = emptyList()

        fun eventId(eventId: Long?) = apply { this.eventId = eventId }
        fun type(type: OptionGroupType?) = apply { this.type = type }
        fun name(name: String?) = apply { this.name = name }
        fun description(description: String?) = apply { this.description = description }
        fun isEssential(isEssential: Boolean?) = apply { this.isEssential = isEssential }
        fun options(options: List<Option>) = apply { this.options = options }
        fun build() = OptionGroup(eventId, type, name, description, isEssential, options)
    }
}
