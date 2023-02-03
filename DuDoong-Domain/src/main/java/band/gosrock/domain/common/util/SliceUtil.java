package band.gosrock.domain.common.util;


import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class SliceUtil {
    // 리스트를 슬라이스로 변환
    public static <T> Slice<T> valueOf(List<T> contents, Pageable pageable) {
        boolean hasNext = hasNext(contents, pageable);
        return new SliceImpl<>(
                hasNext ? getContent(contents, pageable) : contents, pageable, hasNext);
    }

    // 다음 페이지 있는지 확인
    private static <T> boolean hasNext(List<T> content, Pageable pageable) {
        return pageable.isPaged() && content.size() > pageable.getPageSize();
    }

    // 데이터 1개 빼고 반환
    private static <T> List<T> getContent(List<T> content, Pageable pageable) {
        return content.subList(0, pageable.getPageSize());
    }
}
