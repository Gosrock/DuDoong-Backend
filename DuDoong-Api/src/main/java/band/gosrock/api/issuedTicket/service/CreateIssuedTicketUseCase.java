package band.gosrock.api.issuedTicket.service;

import band.gosrock.api.issuedTicket.dto.request.PostIssuedTicketRequestDTOs;
import band.gosrock.api.issuedTicket.dto.response.IssuedTicketDTO;
import band.gosrock.api.issuedTicket.dto.response.PostIssuedTicketResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final IssuedTicketRepository issuedTicketRepository;

    public PostIssuedTicketResponse execute(PostIssuedTicketRequestDTOs body) {
        List<IssuedTicketDTO> issuedTickets = body.getPostIssuedTicketRequests().stream()
            .map(postIssuedTicketRequest -> {
                IssuedTicket issuedTicket = IssuedTicket.createIssuedTicket(
                    postIssuedTicketRequest.getEventId(), postIssuedTicketRequest.getOrderLineId(),
                    postIssuedTicketRequest.getUserId(), postIssuedTicketRequest.getTicketItemId());
                issuedTicketRepository.save(issuedTicket);
                return new IssuedTicketDTO(issuedTicket);
            }).toList();
        return new PostIssuedTicketResponse(issuedTickets);
    }
}
