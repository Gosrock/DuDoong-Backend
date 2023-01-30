package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.exception.OptionNotFoundException;
import band.gosrock.domain.domains.ticket_item.repository.OptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class OptionAdaptor {

    private final OptionRepository optionRepository;

    public Option queryOption(Long optionId) {
        return optionRepository
                .findById(optionId)
                .orElseThrow(() -> OptionNotFoundException.EXCEPTION);
    }

    public List<Option> findAllByIds(List<Long> ids) {
        return optionRepository.findAllById(ids);
    }
}
