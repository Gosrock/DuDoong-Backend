package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.host.domain.HostContactType

data class HostContactVo(
    val type: HostContactType,
    val value: String,
)
