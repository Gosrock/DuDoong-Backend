package band.gosrock.domain.domains.ticket_item.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["item_id", "option_group_id"])])
@Entity(name = "tbl_item_option_group")
class ItemOptionGroup() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_option_group_id")
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", updatable = false)
    var item: TicketItem? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", updatable = false)
    var optionGroup: OptionGroup? = null
        protected set

    constructor(item: TicketItem?, optionGroup: OptionGroup?) : this() {
        this.item = item
        this.optionGroup = optionGroup
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var item: TicketItem? = null
        private var optionGroup: OptionGroup? = null

        fun item(item: TicketItem?) = apply { this.item = item }
        fun optionGroup(optionGroup: OptionGroup?) = apply { this.optionGroup = optionGroup }
        fun build() = ItemOptionGroup(item, optionGroup)
    }
}
