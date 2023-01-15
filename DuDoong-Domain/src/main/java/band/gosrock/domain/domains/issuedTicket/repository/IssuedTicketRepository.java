package band.gosrock.domain.domains.issuedTicket.repository;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedTicketRepository
        extends JpaRepository<IssuedTicket, Long>, IssuedTicketRepositoryCustom {
    List<IssuedTicket> findAllByOrderLineId(Long orderLineId);

    Page<IssuedTicket> findAllByEvent_IdOrderByIdDesc(Long eventId, Pageable pageable);

    Page<IssuedTicket> findAllByEvent_IdAndUser_Profile_NameContaining(
            Long eventId, String userName, Pageable pageable);

    Page<IssuedTicket> findAllByEvent_IdAndUser_Profile_PhoneNumberContaining(
            Long eventId, String phoneNumber, Pageable pageable);
}
