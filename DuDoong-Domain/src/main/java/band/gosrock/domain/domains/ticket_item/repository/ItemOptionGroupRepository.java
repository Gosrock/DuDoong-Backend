package band.gosrock.domain.domains.ticket_item.repository;


import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOptionGroupRepository extends JpaRepository<ItemOptionGroup, Long> {

    Boolean existsByItemIdAndOptionGroupId(Long itemId, Long optionGroupId);
}
