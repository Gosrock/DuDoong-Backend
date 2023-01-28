package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.repository.ItemOptionGroupRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class ItemOptionGroupAdaptor {

    private final ItemOptionGroupRepository itemOptionGroupRepository;

    public Boolean existsQueryItemOptionGroup(Long ticketItemId, Long optionGroupId) {
        return itemOptionGroupRepository.existsByItemIdAndOptionGroupId(
                ticketItemId, optionGroupId);
    }

    public ItemOptionGroup save(ItemOptionGroup itemOptionGroup) {
        return itemOptionGroupRepository.save(itemOptionGroup);
    }
}
