package band.gosrock.domain.domains.ticket_item.domain;


import band.gosrock.domain.domains.event.domain.Event;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private OptionGroupType type;

    // 옵션 그룹 이름
    private String name;

    // 옵션 그룹 설명
    private String description;

    // 필수 응답 여부
    private Boolean isEssential;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id")
    private List<Option> options = new ArrayList<>();

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
        this.options = options;
    }
}
