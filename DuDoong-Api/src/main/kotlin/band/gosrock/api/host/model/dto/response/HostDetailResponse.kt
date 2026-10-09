package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.domains.host.domain.Host
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema

data class HostDetailResponse(
    @field:Schema(description = "호스트 정보")
    @field:JsonUnwrapped
    val hostInfo: HostInfoVo,

    @field:Schema(description = "마스터 유저의 정보")
    val masterUser: HostMemberResponse,

    @field:Schema(description = "호스트 유저 정보")
    val hostUsers: List<HostMemberResponse>,

    @field:Schema(description = "슬랙 알람 url. 매니저 이상에게만 내려가고 그 외에는 null")
    val slackUrl: String?,

    @field:Schema(description = "파트너쉽 여부")
    val partner: Boolean,
) {
    companion object {
        fun of(host: Host, members: List<HostMemberResponse>, showSlackUrl: Boolean): HostDetailResponse {
            var masterUser: HostMemberResponse? = null
            val hostUserList = mutableListOf<HostMemberResponse>()

            members.forEach { member ->
                if (member.userId == host.masterUserId) {
                    masterUser = member
                } else {
                    hostUserList.add(member)
                }
            }

            return HostDetailResponse(
                hostInfo = HostInfoVo.from(host),
                masterUser = masterUser!!,
                hostUsers = hostUserList,
                partner = host.partner,
                slackUrl = if (showSlackUrl) host.slackUrl else null,
            )
        }
    }
}
