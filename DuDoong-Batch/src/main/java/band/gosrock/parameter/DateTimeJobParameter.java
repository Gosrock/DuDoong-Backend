package band.gosrock.parameter;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class DateTimeJobParameter {
    private LocalDateTime time;

    @Value("#{jobParameters[dateTime]}")
    public void setDateTime(String dateTime) throws JobParametersInvalidException {
        // 인자가 없다면 now 로 설정
        if (Objects.isNull(dateTime)) this.time = LocalDateTime.now();
        else {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                this.time = LocalDateTime.parse(dateTime, formatter);
            } catch (Exception e) {
                throw new JobParametersInvalidException("올바르지 않은 시간 형식입니다");
            }
        }
    }
}
