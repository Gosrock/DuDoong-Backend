package band.gosrock.domain.domains.ticket_item.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.ticket_item.adaptor.ItemOptionGroupAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.DuplicatedItemOptionGroupException;
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionGroupException;
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemOptionGroupService {

    private final ItemOptionGroupAdaptor itemOptionGroupAdaptor;

    public void checkEventId(Long eventId, TicketItem ticketItem, OptionGroup optionGroup) {
        if (!ticketItem.getEvent().getId().equals(eventId)) {
            throw InvalidTicketItemException.EXCEPTION;
        }
        if (!optionGroup.getEvent().getId().equals(eventId)) {
            throw InvalidOptionGroupException.EXCEPTION;
        }
    }

    public void checkApply(TicketItem ticketItem, OptionGroup optionGroup) {
        if (itemOptionGroupAdaptor.existsQueryItemOptionGroup(
                ticketItem.getId(), optionGroup.getId())) {
            throw DuplicatedItemOptionGroupException.EXCEPTION;
        }
    }

    @Transactional
    public ItemOptionGroup createItemOptionGroup(ItemOptionGroup itemOptionGroup) {
        return itemOptionGroupAdaptor.save(itemOptionGroup);
    }
}
