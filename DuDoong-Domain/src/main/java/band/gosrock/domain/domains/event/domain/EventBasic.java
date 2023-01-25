package band.gosrock.domain.domains.event.domain;


import java.time.LocalDateTime;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventBasic {
    private String name;
    private LocalDateTime startAt;
    private Long runTime;

    @Builder
    public EventBasic(String name, LocalDateTime startAt, Long runTime) {
        this.name = name;
        this.startAt = startAt;
        this.runTime = runTime;
    }
}
