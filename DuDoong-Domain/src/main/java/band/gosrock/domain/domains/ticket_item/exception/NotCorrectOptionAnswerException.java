package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotCorrectOptionAnswerException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotCorrectOptionAnswerException();

    private NotCorrectOptionAnswerException() {
        super(TicketItemErrorCode.OPTION_ANSWER_NOT_CORRECT);
    }
}
