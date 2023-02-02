package band.gosrock.api.common.slice;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@AllArgsConstructor
public class SliceParam {
    @Schema(description = "lastId 부터 원소를 불러옵니다")
    @Min(value = -1)
    private Long lastId;

    @Schema(description = "한 번 조회당 원소 개수")
    @Positive
    private Integer size;

    @Schema(description = "정렬 조건")
    private String sort;

    @Schema(description = "정렬 순서 (ASC | DESC)")
    private Sort.Direction direction;

    public Pageable toPageable() {
        // RequestParam 에 빈 값은 null 로 할당되어 검증 필요
        if (size == null) size = 10;
        if (direction == null) direction = Sort.Direction.DESC;
        if (sort == null) sort = "id";
        return PageRequest.of(0, size, Sort.by(direction, sort));
    }

    public static Pageable pageableOf(SliceParam sliceParam) {
        return sliceParam.toPageable();
    }
}
