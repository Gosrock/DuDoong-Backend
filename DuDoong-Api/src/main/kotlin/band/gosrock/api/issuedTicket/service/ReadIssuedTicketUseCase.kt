package band.gosrock.api.issuedTicket.service

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
     * @param userId 현재 사용자 id
     * @param uuid 발급 티켓 id
     * @return RetrieveIssuedTicketDetailResponse
     */
    fun execute(userId: Long, uuid: String): RetrieveIssuedTicketDetailResponse {
        return issuedTicketMapper.toIssuedTicketDetailResponse(userId, uuid)
    }
}
