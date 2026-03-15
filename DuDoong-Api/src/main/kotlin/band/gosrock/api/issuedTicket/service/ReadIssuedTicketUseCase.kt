package band.gosrock.api.issuedTicket.service

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse
import band.gosrock.api.issuedTicket.mapper.IssuedTicketMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class ReadIssuedTicketUseCase(
    private val issuedTicketMapper: IssuedTicketMapper,
) {

    /**
     * 발급 티켓 상세 정보 API
     *
     * @param uuid 발급 티켓 id
     * @return RetrieveIssuedTicketDetailResponse
     */
    fun execute(uuid: String): RetrieveIssuedTicketDetailResponse {
        val currentUserId = SecurityUtils.getCurrentUserId()
        return issuedTicketMapper.toIssuedTicketDetailResponse(currentUserId, uuid)
    }
}
