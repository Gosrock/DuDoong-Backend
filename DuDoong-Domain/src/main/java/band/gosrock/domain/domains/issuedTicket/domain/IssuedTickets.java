package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import java.util.List;
import lombok.Getter;

@Getter
public class IssuedTickets {

    private List<IssuedTicket> issuedTickets;

    public IssuedTickets(List<IssuedTicket> issuedTickets) {
        this.issuedTickets = issuedTickets;
    }

    public static IssuedTickets from(List<IssuedTicket> issuedTickets) {
        return new IssuedTickets(issuedTickets);
    }

    public List<String> getNos() {
        return this.issuedTickets.stream().map(IssuedTicket::getIssuedTicketNo).toList();
    }

    public int getTotalQuantity() {
        return issuedTickets.size();
    }

    public String getTicketNoName() {
        List<String> nos = getNos();
        Integer size = nos.size();
        // 없을 경우긴 함
        if (size.equals(0)) return "";
        else if (size.equals(1)) return String.format("%s (%d매)", nos.get(0), size);
        else return String.format("%s ~ %s (%d매)", nos.get(0), nos.get(size - 1), size);
    }

    public IssuedTicketsStage getIssuedTicketsStage(Event event) {
        if (getTotalQuantity() == 0) return IssuedTicketsStage.APPROVE_WAITING;
        List<IssuedTicketStatus> issuedTicketStatuses = getIssuedTicketStatuses();
        if (isCanceled(issuedTicketStatuses)) {
            return IssuedTicketsStage.CANCELED;
        }
        // 정상 발급된 티켓이 . 지난 공연이라면 지난 공연으로 통일.
        if (event.isClosed()) {
            return IssuedTicketsStage.PASSED_EVENT;
        }

        if (isBeforeEntrance(issuedTicketStatuses)) return IssuedTicketsStage.BEFORE_ENTRANCE;
        if (isAfterEntrance(issuedTicketStatuses)) return IssuedTicketsStage.AFTER_ENTRANCE;
        return IssuedTicketsStage.ENTERING;
    }

    private static boolean isCanceled(List<IssuedTicketStatus> issuedTicketStatuses) {
        return issuedTicketStatuses.stream().anyMatch(IssuedTicketStatus::isCanceled);
    }

    private static boolean isBeforeEntrance(List<IssuedTicketStatus> issuedTicketStatuses) {
        return issuedTicketStatuses.stream().allMatch(IssuedTicketStatus::isBeforeEntrance);
    }

    private static boolean isAfterEntrance(List<IssuedTicketStatus> issuedTicketStatuses) {
        return issuedTicketStatuses.stream().allMatch(IssuedTicketStatus::isAfterEntrance);
    }

    public List<IssuedTicketStatus> getIssuedTicketStatuses() {
        return this.issuedTickets.stream().map(IssuedTicket::getIssuedTicketStatus).toList();
    }

    public List<IssuedTicketInfoVo> getIssuedTicketInfoVos() {
        return this.issuedTickets.stream().map(IssuedTicket::toIssuedTicketInfoVo).toList();
    }
}
