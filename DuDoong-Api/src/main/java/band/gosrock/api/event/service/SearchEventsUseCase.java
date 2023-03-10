package band.gosrock.api.event.service;


import band.gosrock.api.common.slice.SliceResponse;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** 해당 호스트 유저가 관리중인 이벤트 리스트를 불러오는 유즈케이스 */
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchEventsUseCase {
    private final EventMapper eventMapper;

    public SliceResponse<EventResponse> execute(String keyword, Pageable pageable) {
        return SliceResponse.of(eventMapper.toEventResponseSliceByKeyword(keyword, pageable));
    }
}
