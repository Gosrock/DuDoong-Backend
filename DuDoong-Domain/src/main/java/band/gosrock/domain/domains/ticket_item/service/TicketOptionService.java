package band.gosrock.domain.domains.ticket_item.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketOptionService {

    private final OptionGroupAdaptor optionGroupAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional
    public OptionGroup createTicketOption(OptionGroup optionGroup) {
        return optionGroupAdaptor.save(optionGroup);
    }

    @Transactional
    public void softDeleteOptionGroup(Long eventId, Long optionGroupId) {

        OptionGroup optionGroup = optionGroupAdaptor.queryOptionGroup(optionGroupId);
        // 해당 eventId에 속해 있는 옵션그룹이 맞는지 확인
        optionGroup.validateEventId(eventId);

        List<TicketItem> ticketItems = ticketItemAdaptor.findAllByEventId(eventId);
        optionGroup.softDeleteOptionGroup(ticketItems);
        optionGroupAdaptor.save(optionGroup);
    }
}
