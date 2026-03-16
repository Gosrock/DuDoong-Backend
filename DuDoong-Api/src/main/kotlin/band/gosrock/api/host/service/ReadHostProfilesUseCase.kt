package band.gosrock.api.host.service

import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.api.host.model.dto.response.HostProfileResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReadHostProfilesUseCase(
    private val hostAdaptor: HostAdaptor,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long, pageable: Pageable): SliceResponse<HostProfileResponse> {
        return SliceResponse.of(
            hostAdaptor
                .querySliceHostsByUserId(userId, pageable)
                .map { host -> HostProfileResponse.of(host, userId) }
        )
    }
}
