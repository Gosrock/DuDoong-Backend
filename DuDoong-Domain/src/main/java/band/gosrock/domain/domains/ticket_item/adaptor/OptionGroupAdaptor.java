package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus;
import band.gosrock.domain.domains.ticket_item.exception.OptionGroupNotFoundException;
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class OptionGroupAdaptor {

    private final OptionGroupRepository optionGroupRepository;

    public OptionGroup queryOptionGroup(Long optionGroupId) {
        return optionGroupRepository
                .findByIdAndOptionGroupStatus(optionGroupId, OptionGroupStatus.VALID)
                .orElseThrow(() -> OptionGroupNotFoundException.EXCEPTION);
    }

    public List<OptionGroup> findAllByEventId(Long eventId) {
        return optionGroupRepository.findAllByEvent_IdAndOptionGroupStatus(
                eventId, OptionGroupStatus.VALID);
    }

    public OptionGroup save(OptionGroup optionGroup) {
        return optionGroupRepository.save(optionGroup);
    }
}
