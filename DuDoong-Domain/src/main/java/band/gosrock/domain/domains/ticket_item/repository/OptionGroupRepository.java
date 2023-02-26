package band.gosrock.domain.domains.ticket_item.repository;


import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, Long> {

    Optional<OptionGroup> findByIdAndOptionGroupStatus(
            Long optionGroupId, OptionGroupStatus status);

    List<OptionGroup> findAllByEventIdAndOptionGroupStatus(Long eventId, OptionGroupStatus status);
}
