package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketOptionAnswerAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final IssuedTicketOptionAnswerAdaptor issuedTicketOptionAnswerAdaptor;
    private final EventAdaptor eventAdaptor;

    // Todo 둘 중 어떤 로직이 더 나은지 비교해주세요
    //    @Transactional
    //    public void createIssuedTicket(CreateIssuedTicketRequestDTOs
    // createIssuedTicketRequestDTOs) {
    //        List<IssuedTicketDTO> issuedTickets =
    //                createIssuedTicketRequestDTOs.getCreateIssuedTicketRequests().stream()
    //                        .map(
    //                                createIssuedTicketRequest -> {
    //                                    IssuedTicket issuedTicket =
    //                                            IssuedTicket.create(createIssuedTicketRequest);
    //                                    /*
    //                                    티켓 옵션 답변 저장
    //                                     */
    //                                    List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers =
    //
    // createIssuedTicketRequest.getOptionAnswers().stream()
    //                                                    .map(
    //                                                            IssuedTicketOptionAnswer
    //
    // ::orderOptionAnswerToIssuedTicketOptionAnswer)
    //                                                    .toList();
    //                                    /*
    //                                    티켓 옵션 답변 매핑
    //                                     */
    //                                    issuedTicket.addOptionAnswers(issuedTicketOptionAnswers);
    //                                    IssuedTicket saveIssuedTicket =
    //                                            issuedTicketAdaptor.save(issuedTicket);
    //                                    issuedTicketOptionAnswerAdaptor.saveAll(
    //                                            issuedTicketOptionAnswers);
    //                                    return new IssuedTicketDTO(saveIssuedTicket);
    //                                })
    //                        .toList();
    //    }

    @Transactional
    public void createIssuedTicket(List<CreateIssuedTicketDTO> createIssuedTicketDTOs) {
        createIssuedTicketDTOs.forEach(
                dto -> {
                    CreateIssuedTicketResponse responseDTO =
                            IssuedTicket.orderLineItemToIssuedTickets(dto);
                    issuedTicketAdaptor.saveAll(responseDTO.getIssuedTickets());
                    issuedTicketOptionAnswerAdaptor.saveAll(
                            responseDTO.getIssuedTicketOptionAnswers());
                });
    }
}
