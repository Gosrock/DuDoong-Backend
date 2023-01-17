package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.exception.OptionNotFoundException;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class OptionAdaptor {

    private final OptionRepository optionRepository;

    public Option find(Long optionId) {
        return optionRepository
                .findById(optionId)
                .orElseThrow(() -> OptionNotFoundException.EXCEPTION);
    }
}
