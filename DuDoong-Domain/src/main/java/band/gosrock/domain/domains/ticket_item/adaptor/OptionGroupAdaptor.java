package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.exception.OptionGroupNotFoundException;
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class OptionGroupAdaptor {

    private final OptionGroupRepository optionGroupRepository;

    public OptionGroup queryOptionGroup(Long optionGroupId) {
        return optionGroupRepository
                .findById(optionGroupId)
                .orElseThrow(() -> OptionGroupNotFoundException.EXCEPTION);
    }

    public OptionGroup save(OptionGroup optionGroup) {
        return optionGroupRepository.save(optionGroup);
    }
}
