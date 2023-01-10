package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.request.PostIssuedTicketRequestDTOs;
import band.gosrock.domain.domains.issuedTicket.dto.response.IssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.PostIssuedTicketResponse;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketUserNotMatchedException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final IssuedTicketAdaptor issuedTicketAdaptor;

    @Transactional
    public PostIssuedTicketResponse createIssuedTicket(
            PostIssuedTicketRequestDTOs postIssuedTicketRequestDTOs) {
        List<IssuedTicketDTO> issuedTickets =
                postIssuedTicketRequestDTOs.getPostIssuedTicketRequests().stream()
                        .map(
                                postIssuedTicketRequest -> {
                                    IssuedTicket issuedTicket =
                                            IssuedTicket.create(postIssuedTicketRequest);
                                    /*
                                    Todo: 발급 티켓 관련 옵션들 처리 로직 어떻게 전달 받을지 논의 후 작성 예정
                                     */
                                    //                issuedTicketRepository.save(issuedTicket);
                                    issuedTicketAdaptor.save(issuedTicket);
                                    return new IssuedTicketDTO(issuedTicket);
                                })
                        .toList();
        return new PostIssuedTicketResponse(issuedTickets);
    }

    @Transactional(readOnly = true)
    public IssuedTicket retrieveIssuedTicket(Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.find(issuedTicketId);
        if (!Objects.equals(issuedTicket.getUserId(), currentUserId)) {
            throw IssuedTicketUserNotMatchedException.EXCEPTION;
        }
        return issuedTicket;
    }
}
