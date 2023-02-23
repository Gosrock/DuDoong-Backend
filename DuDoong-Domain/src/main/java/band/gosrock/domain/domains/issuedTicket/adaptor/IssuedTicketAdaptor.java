package band.gosrock.domain.domains.issuedTicket.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketUserNotMatchedException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Adaptor
@RequiredArgsConstructor
public class IssuedTicketAdaptor {

    private final IssuedTicketRepository issuedTicketRepository;

    public IssuedTicket save(IssuedTicket issuedTicket) {
        return issuedTicketRepository.save(issuedTicket);
    }

    public void saveAll(List<IssuedTicket> issuedTickets) {
        issuedTicketRepository.saveAll(issuedTickets);
    }

    public IssuedTicket findForUser(Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket =
                issuedTicketRepository
                        .find(issuedTicketId)
                        .orElseThrow(() -> IssuedTicketNotFoundException.EXCEPTION);
        if (!Objects.equals(issuedTicket.getUserInfo().getUserId(), currentUserId)) {
            throw IssuedTicketUserNotMatchedException.EXCEPTION;
        }
        return issuedTicket;
    }

    public IssuedTicket queryIssuedTicket(Long issuedTicketId) {
        return issuedTicketRepository
                .find(issuedTicketId)
                .orElseThrow(() -> IssuedTicketNotFoundException.EXCEPTION);
    }

    public Boolean existsByEventId(Long eventId) {
        return issuedTicketRepository.existsByEventId(eventId);
    }

    public Page<IssuedTicket> searchIssuedTicket(
            Pageable page, FindEventIssuedTicketsCondition condition) {
        return issuedTicketRepository.searchToPage(condition, page);
    }

    public void cancel(IssuedTicket issuedTicket) {
        issuedTicket.cancel();
    }

    public List<IssuedTicket> findAllByOrderUuid(String orderUuid) {
        return issuedTicketRepository.findAllByOrderUuid(orderUuid);
    }

    public IssuedTickets findOrderLineIssuedTickets(Long orderLineId) {
        return IssuedTickets.from(issuedTicketRepository.findAllByOrderLineId(orderLineId));
    }

    public IssuedTickets findOrderIssuedTickets(String orderUuid) {
        return IssuedTickets.from(issuedTicketRepository.findAllByOrderUuid(orderUuid));
    }

    /** 유저 아이디와 티켓 아이템 아이디를 인자로받아 유저가 현재까지 발급한 정상 티켓을 불러온다. */
    public Long countPaidTicket(Long userId, Long itemId) {
        return issuedTicketRepository.countPaidTicket(userId, itemId);
    }

    public IssuedTicket queryByIssuedTicketNo(String issuedTicketNo) {
        return issuedTicketRepository
                .findByIssuedTicketNo(issuedTicketNo)
                .orElseThrow(() -> IssuedTicketNotFoundException.EXCEPTION);
    }
}
