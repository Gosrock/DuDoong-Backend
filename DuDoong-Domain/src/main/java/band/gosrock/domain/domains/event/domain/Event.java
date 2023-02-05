package band.gosrock.domain.domains.event.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.*;
import band.gosrock.domain.domains.event.exception.CannotModifyEventBasicException;
import band.gosrock.domain.domains.event.exception.EventCannotEndBeforeStartException;
import band.gosrock.domain.domains.event.exception.EventNotOpenException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_event")
public class Event extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    // 호스트 정보
    private Long hostId;

    @Embedded private EventBasic eventBasic;

    @Embedded private EventPlace eventPlace;

    @Embedded private EventDetail eventDetail;

    // 이벤트 상태
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.PREPARING;

    private Boolean isUpdated = false;

    /*********** 미확정된 정보 ***********/
    // 공연 진행 시간

    // 예매 시작 시각
    private LocalDateTime ticketingStartAt;

    // 예매 종료 시각
    private LocalDateTime ticketingEndAt;
    /*********** 미확정된 정보 ***********/

    public LocalDateTime getStartAt() {
        if (this.eventBasic == null) {
            return null;
        }
        return this.getEventBasic().getStartAt();
    }

    public LocalDateTime getEndAt() {
        if (this.eventBasic == null) {
            return null;
        }
        return this.getEventBasic().endAt();
    }

    public Boolean hasEventBasic() {
        return this.eventBasic != null && this.eventBasic.isUpdated();
    }

    public Boolean hasEventPlace() {
        return this.eventPlace != null && this.eventPlace.isUpdated();
    }

    public Boolean hasEventDetail() {
        return this.eventDetail != null && this.eventDetail.isUpdated();
    }

    public Boolean isPreparing() {
        return this.status == EventStatus.PREPARING;
    }

    /** 티켓팅 시작과 종료 시간을 지정 */
    public void setTicketingTime(LocalDateTime startAt, LocalDateTime endAt) {
        // 이벤트 종료가 시작보다 빠르면 안됨
        if (startAt.isAfter(endAt)) {
            throw EventCannotEndBeforeStartException.EXCEPTION;
        }
        this.ticketingStartAt = startAt;
        this.ticketingEndAt = endAt;
    }

    public void setEventBasic(EventBasic eventBasic) {
        if (isUpdated) {
            throw CannotModifyEventBasicException.EXCEPTION;
        }
        this.isUpdated = true;
        this.eventBasic = eventBasic;
    }

    public void setEventDetail(EventDetail eventDetail) {
        this.eventDetail = eventDetail;
    }

    public void setEventPlace(EventPlace eventPlace) {
        // 정보 한 번 등록시 변경 불가
        this.eventPlace = eventPlace;
    }

    @Builder
    public Event(Long hostId, String name, LocalDateTime startAt, Long runTime) {
        this.hostId = hostId;
        this.eventBasic = EventBasic.builder().name(name).startAt(startAt).runTime(runTime).build();
    }

    public void validateStatusOpen() {
        if (status != EventStatus.OPEN) {
            throw EventNotOpenException.EXCEPTION;
        }
    }

    public void validateTicketingTime() {
        if (!isTimeBeforeStartAt()) {
            throw EventTicketingTimeIsPassedException.EXCEPTION;
        }
    }

    public Boolean isRefundDateNotPassed() {
        return toRefundInfoVo().getAvailAble();
    }

    public boolean isTimeBeforeStartAt() {
        return LocalDateTime.now().isBefore(getStartAt());
    }

    public RefundInfoVo toRefundInfoVo() {
        return RefundInfoVo.from(getEndAt());
    }

    public EventInfoVo toEventInfoVo() {
        return EventInfoVo.from(this);
    }

    public EventDetailVo toEventDetailVo() {
        return EventDetailVo.from(this);
    }

    public EventBasicVo toEventBasicVo() {
        return EventBasicVo.from(this);
    }

    public EventProfileVo toEventProfileVo() {
        return EventProfileVo.from(this);
    }

    public EventPlaceVo toEventPlaceVo() {
        return EventPlaceVo.from(this);
    }

    public void prepare() {
        // TODO : 오픈할수 있는 상태인지 검증필요함.
        this.status = EventStatus.PREPARING;
    }

    public void open() {
        // TODO : 오픈할수 있는 상태인지 검증필요함.
        this.status = EventStatus.OPEN;
    }

    public void calculate() {
        // TODO : 오픈할수 있는 상태인지 검증필요함.
        this.status = EventStatus.CALCULATING;
    }

    public void close() {
        // TODO : 오픈할수 있는 상태인지 검증필요함.
        this.status = EventStatus.OPEN;
    }
}
