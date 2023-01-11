package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketOptionAnswerAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketRequestDTOs;
import band.gosrock.domain.domains.issuedTicket.dto.response.IssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketUserNotMatchedException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final IssuedTicketOptionAnswerAdaptor issuedTicketOptionAnswerAdaptor;

    @Transactional
    public CreateIssuedTicketResponse createIssuedTicket(
            CreateIssuedTicketRequestDTOs createIssuedTicketRequestDTOs) {
        List<IssuedTicketDTO> issuedTickets =
                createIssuedTicketRequestDTOs.getCreateIssuedTicketRequests().stream()
                        .map(
                                createIssuedTicketRequest -> {
                                    IssuedTicket issuedTicket =
                                            IssuedTicket.create(createIssuedTicketRequest);
                                    /*
                                    티켓 옵션 답변 저장
                                     */
                                    List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = createIssuedTicketRequest.getOptionAnswers()
                                        .stream().map(
                                            IssuedTicketOptionAnswer::createIssuedTicketOptionAnswer)
                                        .toList();
                                    /*
                                    티켓 옵션 답변 매핑
                                     */
                                    issuedTicket.addOptionAnswers(issuedTicketOptionAnswers);
                                    IssuedTicket saveIssuedTicket = issuedTicketAdaptor.save(issuedTicket);
                                    issuedTicketOptionAnswerAdaptor.saveAll(
                                        issuedTicketOptionAnswers);
                                    return new IssuedTicketDTO(saveIssuedTicket);
                                })
                        .toList();
        return new CreateIssuedTicketResponse(issuedTickets);
    }

    /**
     * 발급 티켓 정보 가져오기 비즈니스 로직
     *
     * @param currentUserId 현재 접근 유저 id
     * @param issuedTicketId 검색하고자 하는 발급 티켓 id
     * @return IssuedTicketDTO (transaction 상태에서만 지연 로딩 프록시 객체를 갖고올 수 있으므로 DTO로 묶어서 반환)
     */
    @Transactional(readOnly = true)
    public IssuedTicketDTO retrieveIssuedTicket(Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.find(issuedTicketId);
        if (!Objects.equals(issuedTicket.getUser().getId(), currentUserId)) {
            throw IssuedTicketUserNotMatchedException.EXCEPTION;
        }
        return new IssuedTicketDTO(issuedTicket);
    }

    /**
     * 발급 티켓 리스트 가져오기 비즈니스 로직
     * @param page 페이지 번호
     * @param eventId 이벤트 id
     * @param userName 검색할 유저 이름
     * @return Page<IssuedTicket>
     */
    @Transactional(readOnly = true)
    public Page<IssuedTicket> retrieveIssuedTickets(Long page, Long eventId, String userName) {
        PageRequest pageRequest = PageRequest.of(Math.toIntExact(page-1), 10, Sort.by("id").descending());
        if (userName == null) {
            return issuedTicketAdaptor.findAllByEvent(pageRequest, eventId);
        } else {
            return issuedTicketAdaptor.findAllByEventAndUserName(pageRequest, eventId, userName);
        }
    }
}
