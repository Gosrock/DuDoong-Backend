package band.gosrock.domain.domains.event.domain;

import static band.gosrock.domain.domains.event.domain.EventStatus.*;

import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.domain.common.aop.domainEvent.Events;
import band.gosrock.domain.common.events.event.EventContentChangeEvent;
import band.gosrock.domain.common.events.event.EventCreationEvent;
import band.gosrock.domain.common.events.event.EventStatusChangeEvent;
import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.*;
import band.gosrock.domain.domains.event.exception.*;
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
    private EventStatus status = PREPARING;

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
        return this.status == PREPARING;
    }

    public void setEventBasic(EventBasic eventBasic) {
        this.eventBasic = eventBasic;
    }

    public void setEventDetail(EventDetail eventDetail) {
        this.eventDetail = eventDetail;
        Events.raise(EventContentChangeEvent.of(this));
    }

    public void setEventPlace(EventPlace eventPlace) {
        this.eventPlace = eventPlace;
    }

    @Builder
    public Event(Long hostId, String name, LocalDateTime startAt, Long runTime) {
        this.hostId = hostId;
        this.eventBasic = EventBasic.builder().name(name).startAt(startAt).runTime(runTime).build();
        Events.raise(EventCreationEvent.of(hostId, name));
    }

    public void validateStatusOpen() {
        if (status != OPEN) {
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
        updateStatus(PREPARING, AlreadyPreparingStatusException.EXCEPTION);
    }

    public void open() {
        updateStatus(OPEN, AlreadyOpenStatusException.EXCEPTION);
    }

    public void calculate() {
        updateStatus(CALCULATING, AlreadyCalculatingStatusException.EXCEPTION);
    }

    public void close() {
        updateStatus(CLOSED, AlreadyCloseStatusException.EXCEPTION);
    }

    private void updateStatus(EventStatus status, DuDoongCodeException exception) {
        if (this.status == status) throw exception;
        this.status = status;
        Events.raise(EventStatusChangeEvent.of(this));
    }
}
