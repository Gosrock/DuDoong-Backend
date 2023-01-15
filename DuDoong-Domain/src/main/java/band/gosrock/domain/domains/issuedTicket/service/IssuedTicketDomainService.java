package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketOptionAnswerAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condtion.IssuedTicketCondition;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketUserNotMatchedException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final IssuedTicketOptionAnswerAdaptor issuedTicketOptionAnswerAdaptor;

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

    /**
     * 발급 티켓 정보 가져오기 비즈니스 로직
     *
     * @param currentUserId 현재 접근 유저 id
     * @param issuedTicketId 검색하고자 하는 발급 티켓 id
     * @return IssuedTicketDTO (transaction 상태에서만 지연 로딩 프록시 객체를 갖고올 수 있으므로 DTO로 묶어서 반환)
     */
    @Transactional(readOnly = true)
    public IssuedTicket retrieveIssuedTicket(Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.find(issuedTicketId);
        if (!Objects.equals(issuedTicket.getUser().getId(), currentUserId)) {
            throw IssuedTicketUserNotMatchedException.EXCEPTION;
        }
        return issuedTicket;
    }

    /**
     * 발급 티켓 리스트 가져오기 비즈니스 로직
     *
     * @param page 페이지 번호
     * @param eventId 이벤트
     * @param userName 검색할 유저 이름
     * @return Page<IssuedTicket>
     */
    @Transactional(readOnly = true)
    public Page<IssuedTicket> retrieveIssuedTickets(
            Long page, Long eventId, String userName, String phoneNumber) {
        return issuedTicketAdaptor.searchIssuedTicket(
                page, new IssuedTicketCondition(eventId, userName, phoneNumber));
    }
}
