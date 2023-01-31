package band.gosrock.api.event.model.dto.response;


import band.gosrock.domain.domains.event.domain.Event;
import lombok.Builder;
import lombok.Getter;

/** 공연 체크 리스트 응답 DTO */
@Getter
@Builder
public class EventChecklistResponse {
    private Long hostId;
    private Long eventId;
    private String name;
    // 공연 기본 정보 (공연장 위치로 구분) 작성 여부
    private Boolean hasBasic;
    // 공연 상세 정보 작성 여부
    private Boolean hasDetail;
    // 티켓 상품 설정했는지 여부
    private Boolean hasTicketItem;

    public static EventChecklistResponse of(Event event, Boolean hasTicket) {
        return EventChecklistResponse.builder()
                .hostId(event.getHostId())
                .eventId(event.getId())
                .name(event.getEventBasic().getName())
                .hasBasic(event.hasEventBasic() && event.hasEventPlace())
                .hasDetail(event.hasEventDetail())
                .hasTicketItem(hasTicket)
                .build();
    }
}
