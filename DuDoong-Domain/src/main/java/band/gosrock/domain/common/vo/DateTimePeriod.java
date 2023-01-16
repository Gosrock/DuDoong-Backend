package band.gosrock.domain.common.vo;


import java.time.LocalDateTime;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateTimePeriod {
    // 쿠폰 발행 시작 시각
    private LocalDateTime startAt;
    // 쿠폰 발행 마감 시각
    private LocalDateTime endAt;

    @Builder
    public DateTimePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static DateTimePeriod between(LocalDateTime startAt, LocalDateTime endAt) {
        return new DateTimePeriod(startAt, endAt);
    }

    public boolean contains(LocalDateTime datetime) {
        return (datetime.isAfter(startAt) || datetime.equals(startAt))
                && (datetime.isBefore(endAt) || datetime.equals(endAt));
    }
}
