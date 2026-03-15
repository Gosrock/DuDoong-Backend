package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.common.vo.HostUserVo
import band.gosrock.domain.domains.host.domain.Host
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema

data class HostDetailResponse(
    @field:Schema(description = "호스트 정보")
    @field:JsonUnwrapped
    val hostInfo: HostInfoVo,

    @field:Schema(description = "마스터 유저의 정보")
    val masterUser: HostUserVo,

    @field:Schema(description = "호스트 유저 정보")
    val hostUsers: List<HostUserVo>,

    @field:Schema(description = "슬랙 알람 url")
    val slackUrl: String?,

    @field:Schema(description = "파트너쉽 여부")
    val partner: Boolean,
) {
    companion object {
        fun of(host: Host, hostUserVoSet: List<HostUserVo>): HostDetailResponse {
            var masterUser: HostUserVo? = null
            val hostUserVoList = mutableListOf<HostUserVo>()

            hostUserVoSet.forEach { hostUserVo ->
                if (hostUserVo.userInfoVo.userId == host.masterUserId) {
                    masterUser = hostUserVo
                } else {
                    hostUserVoList.add(hostUserVo)
                }
            }

            return HostDetailResponse(
                hostInfo = HostInfoVo.from(host),
                masterUser = masterUser!!,
                hostUsers = hostUserVoList,
                partner = host.partner,
                slackUrl = host.slackUrl,
            )
        }
    }
}
