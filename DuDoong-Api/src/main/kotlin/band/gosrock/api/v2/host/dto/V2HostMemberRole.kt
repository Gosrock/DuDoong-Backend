package band.gosrock.api.v2.host.dto

import band.gosrock.domain.domains.host.domain.HostRole

/**
 * 멤버 추가(H-7)·역할 변경(H-10) 요청의 역할 (#755 C03). 도메인 [HostRole] 은 `@JsonValue` 로 한글을 내보내 Swagger enum 이 한글로 보였다
 * (실제로는 영문 이름만 받음). JSON 값은 예전과 같은 영문 이름. MASTER 는 받지만 지정할 수 없어 예전처럼 HOST_400_10 (양도는 H-12)
 */
enum class V2HostMemberRole(val domain: HostRole) {
    MASTER(HostRole.MASTER),
    MANAGER(HostRole.MANAGER),
    GUEST(HostRole.GUEST),
}
