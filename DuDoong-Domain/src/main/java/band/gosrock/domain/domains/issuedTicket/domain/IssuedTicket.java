package band.gosrock.domain.domains.issuedTicket.domain;

import static band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER;

import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelEntranceException;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotCancelException;
import band.gosrock.domain.domains.issuedTicket.exception.CanNotEntranceException;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketAlreadyEntranceException;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_issued_ticket")
public class IssuedTicket extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_ticket_id")
    private Long id;

    private String issuedTicketNo;

    private Long eventId;

    @Embedded private IssuedTicketUserInfoVo userInfo;

    @Embedded private IssuedTicketItemInfoVo itemInfo;

    /*
    발급 티켓 주문 id (단방향)
     */
    private String orderUuid;

    /*
    발급 티켓의 주문 행 (단방향)
     */
    private Long orderLineId;

    /*
    발급 티켓의 옵션들 (단방향)
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "issued_ticket_id")
    private List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = new ArrayList<>();

    public void addOptionAnswers(List<IssuedTicketOptionAnswer> answers) {
        issuedTicketOptionAnswers.addAll(answers);
    }

    /*
    발급 티켓 uuid
     */
    @Column(nullable = false)
    private String uuid;

    /*
    발급 티켓 가격
     */
    @Embedded private Money price;

    /*
    발급 티켓 상태
     */
    @Enumerated(EnumType.STRING)
    private IssuedTicketStatus issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE;

    @Embedded private IssuedTicketCancelInfoVo cancelInfo;

    /*
    빌더를 통해 객체 생성 시 List는 큰 의미를 두지 않지만
    new ArrayList<>()로 한 번 초기화 시켜주면 NPE를 방지 할 수 있음
     */
    @Builder
    public IssuedTicket(
            Event event,
            User user,
            String orderUuid,
            Long orderLineId,
            TicketItem ticketItem,
            Money price,
            List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        this.eventId = event.getId();
        this.userInfo = IssuedTicketUserInfoVo.from(user);
        this.itemInfo = IssuedTicketItemInfoVo.from(ticketItem);
        this.orderUuid = orderUuid;
        this.orderLineId = orderLineId;
        this.price = price;
        this.issuedTicketOptionAnswers.addAll(issuedTicketOptionAnswers);
        this.cancelInfo = null;
    }

    /** ---------------------------- 생성 관련 메서드 ---------------------------------- */

    /*
    issuedTicket 생성하면서 UUID 생성
     */
    @PrePersist
    public void createUUID() {
        this.uuid = UUID.randomUUID().toString();
    }

    /*
    issuedTicket 생성하면서 티켓 넘버 부여
     */
    @PostPersist
    public void createIssuedTicketNo() {
        this.issuedTicketNo = "T" + Long.sum(NO_START_NUMBER, this.id);
    }

    /*
    발급 티켓 옵션들 합 계산
     */
    public Money sumOptionPrice() {
        return issuedTicketOptionAnswers.stream()
                .map(
                        issuedTicketOptionAnswer ->
                                issuedTicketOptionAnswer.getOption().getAdditionalPrice())
                .reduce(Money.ZERO, Money::plus);
    }

    /*
    issuedTicket VO 변환 메서드
     */
    public IssuedTicketInfoVo toIssuedTicketInfoVo() {
        return IssuedTicketInfoVo.from(this);
    }

    /** ---------------------------- 상태 변환 관련 메서드 ---------------------------------- */

    /*
    발급 티켓 취소 메서드
    티켓이 입장 미완료 상태가 아니면 취소 할 수 없음
     */
    public void cancel() {
        if (this.issuedTicketStatus != IssuedTicketStatus.ENTRANCE_INCOMPLETE) {
            throw CanNotCancelException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.CANCELED;
        this.cancelInfo =
                IssuedTicketCancelInfoVo.of(LocalDateTime.now(), IssuedTicketCancelReason.REFUND);
    }

    public void adminCancel() {
        if (this.issuedTicketStatus != IssuedTicketStatus.ENTRANCE_INCOMPLETE) {
            throw CanNotCancelException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.CANCELED;
        this.cancelInfo =
                IssuedTicketCancelInfoVo.of(
                        LocalDateTime.now(), IssuedTicketCancelReason.ADMIN_CANCEL);
    }

    /*
    발급 티켓으로 입장 시 상태 변환 메서드
    티켓이 입장 미완료 상태가 아니면 입장 할 수 없음
     */
    public void entrance() {
        if (this.issuedTicketStatus == IssuedTicketStatus.CANCELED) {
            throw CanNotEntranceException.EXCEPTION;
        }
        if (this.issuedTicketStatus == IssuedTicketStatus.ENTRANCE_COMPLETED) {
            throw IssuedTicketAlreadyEntranceException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.ENTRANCE_COMPLETED;
    }

    /*
    입장 처리 취소 메서드
    티켓이 입장 완료 상태가 아니면 입장 취소 할 수 없음
     */
    public void entranceCancel() {
        if (this.issuedTicketStatus != IssuedTicketStatus.ENTRANCE_COMPLETED) {
            throw CanNotCancelEntranceException.EXCEPTION;
        }
        this.issuedTicketStatus = IssuedTicketStatus.ENTRANCE_INCOMPLETE;
    }
}
