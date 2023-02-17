package band.gosrock.parameter;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.beans.factory.annotation.Value;

// https://www.youtube.com/watch?v=_nkJkWVH-mo
// 우아한 스프링 배치 이동욱님 22분 참조.
@Getter
@NoArgsConstructor
public class DateJobParameter {

    private LocalDate date;

    // 넘겨온 파라미터는 String 형이고,
    // JobScope 가 레이지로 초기화된다는 점을 이용해서
    // 넘겨온 파라미터를 date 형태로 형 변환 해서 받는다.
    @Value("#{jobParameters[date]}")
    public void setDate(String date) throws JobParametersInvalidException {
        if (Objects.isNull(date)) {
            throw new JobParametersInvalidException("날짜형식의 파라미터가 필요합니다.");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.date = LocalDate.parse(date, formatter);
    }
}
