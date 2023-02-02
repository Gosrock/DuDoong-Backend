package band.gosrock.api.common.slice;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Deprecated
@Getter
@AllArgsConstructor
public class SliceParam {
    @Schema(description = "현재 조회하려는 페이지")
    @Positive
    private Integer page;

    @Schema(description = "한 번 조회당 레코드 개수 (default: 10)")
    @Positive
    private Integer size;

    @Schema(description = "정렬 조건 프로퍼티 (default: \"id\")")
    private String sort;

    @Schema(description = "정렬 순서 (ASC | DESC) (default: DESC)")
    private Sort.Direction direction;

    public Pageable toPageable() {
        // RequestParam 에 빈 값은 null 로 할당되어 검증 필요
        if (page == null) page = 0;
        if (size == null) size = 10;
        if (sort == null) sort = "id";
        if (direction == null) direction = Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sort));
    }

    public static Pageable pageableOf(SliceParam sliceParam) {
        return sliceParam.toPageable();
    }
}
