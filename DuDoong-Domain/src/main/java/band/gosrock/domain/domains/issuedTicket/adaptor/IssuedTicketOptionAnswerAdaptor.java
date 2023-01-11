package band.gosrock.domain.domains.issuedTicket.adaptor;

import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketOptionAnswerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class IssuedTicketOptionAnswerAdaptor {

    private final IssuedTicketOptionAnswerRepository issuedTicketOptionAnswerRepository;

    public void saveAll(List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        issuedTicketOptionAnswerRepository.saveAll(issuedTicketOptionAnswers);
    }
}
