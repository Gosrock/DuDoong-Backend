package band.gosrock.domain.domains.ticket_item.domain;

import static band.gosrock.common.consts.DuDoongStatic.KR_NO;
import static band.gosrock.common.consts.DuDoongStatic.KR_YES;
import static band.gosrock.domain.common.vo.Money.ZERO;
import static band.gosrock.domain.domains.ticket_item.domain.OptionGroupType.*;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionGroupException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_option_group")
public class OptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_group_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 옵션 그룹 응답 형식
    @Enumerated(EnumType.STRING)
    private OptionGroupType type;

    // 옵션 그룹 이름
    private String name;

    // 옵션 그룹 설명
    private String description;

    // 필수 응답 여부
    private Boolean isEssential;

    // 상태
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'VALID'")
    private OptionGroupStatus optionGroupStatus = OptionGroupStatus.VALID;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "optionGroup")
    private final List<Option> options = new ArrayList<>();

    @Builder
    public OptionGroup(
            Event event,
            OptionGroupType type,
            String name,
            String description,
            Boolean isEssential,
            List<Option> options) {
        this.event = event;
        this.type = type;
        this.name = name;
        this.description = description;
        this.isEssential = isEssential;

        this.options.addAll(options);
        options.forEach(option -> option.setOptionGroup(this));
    }

    public void validateEventId(Long eventId) {
        if (!this.getEvent().getId().equals(eventId)) {
            throw InvalidOptionGroupException.EXCEPTION;
        }
    }

    public OptionGroup createTicketOption(Money additionalPrice) {
        OptionGroupType type = this.getType();
        if (type == TRUE_FALSE) {
            this.options.add(Option.create(KR_YES, additionalPrice, this));
            this.options.add(Option.create(KR_NO, ZERO, this));
        } else if (type == SUBJECTIVE) {
            this.options.add(Option.create("", ZERO, this));
        }
        return this;
    }
}
