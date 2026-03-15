package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.HostInfoVo
import band.gosrock.domain.domains.host.domain.Host
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema

data class HostResponse(
    @field:Schema(description = "호스트 프로필")
    @field:JsonUnwrapped
    val profile: HostInfoVo,

    @field:Schema(description = "마스터 유저의 고유 아이디")
    val masterUserId: Long?,

    @field:Schema(description = "파트너쉽 여부")
    val partner: Boolean,
) {
    companion object {
        fun of(host: Host): HostResponse {
            return HostResponse(
                profile = HostInfoVo.from(host),
                masterUserId = host.masterUserId,
                partner = host.partner,
            )
        }
    }
}
