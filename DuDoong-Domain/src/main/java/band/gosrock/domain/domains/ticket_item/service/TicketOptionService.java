package band.gosrock.domain.domains.ticket_item.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketOptionService {

    private final OptionAdaptor optionAdaptor;

    @Transactional
    public OptionGroup createTicketOption(OptionGroup optionGroup) {
        return optionAdaptor.save(optionGroup);
    }
}
