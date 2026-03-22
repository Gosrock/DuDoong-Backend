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
class ItemOptionGroup(
    item: TicketItem? = null,
    optionGroup: OptionGroup? = null,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_option_group_id")
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", updatable = false)
    var item: TicketItem? = item
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", updatable = false)
    var optionGroup: OptionGroup? = optionGroup
        protected set
}
