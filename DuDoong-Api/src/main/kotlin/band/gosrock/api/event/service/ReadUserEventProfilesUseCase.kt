package band.gosrock.api.event.service

import band.gosrock.api.common.UserUtils
import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.api.event.model.dto.response.EventProfileResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

/** 해당 호스트 유저가 관리중인 이벤트 리스트를 불러오는 유즈케이스 */
@UseCase
@Transactional(readOnly = true)
class ReadUserEventProfilesUseCase(
    private val userUtils: UserUtils,
    private val eventMapper: EventMapper
) {
    fun execute(pageable: Pageable): SliceResponse<EventProfileResponse> {
        val userId = userUtils.getCurrentUserId()
        return SliceResponse.of(eventMapper.toEventProfileResponseSlice(userId, pageable))
    }
}
