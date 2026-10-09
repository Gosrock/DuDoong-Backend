package band.gosrock.api.v2.common

import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.domains.host.domain.HostContactType

/** 문의처 응답 (호스트 H-3·공개 상세 P-3·관리 상세 E-3 공통, #755 C23 — 도메인 VO 를 응답에 직접 쓰지 않는다) */
data class V2ContactResponse(
    val type: HostContactType,
    val value: String,
) {
    companion object {
        fun from(vo: HostContactVo): V2ContactResponse = V2ContactResponse(type = vo.type, value = vo.value)
    }
}
