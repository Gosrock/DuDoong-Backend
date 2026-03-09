package band.gosrock.domain.domains.host.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class HostRole(val name2: String, @JsonValue val value: String) {
    // 마스터 (모든 권한)
    MASTER("MASTER", "마스터"),
    // 슈퍼 호스트 (조회, 변경 가능)
    MANAGER("MANAGER", "매니저"),
    // 일반 호스트 (조회만 가능)
    GUEST("GUEST", "게스트");

    companion object {
        // Enum Validation 을 위한 코드, enum 에 속하지 않으면 null 리턴
        @JvmStatic
        @JsonCreator
        fun fromHostRole(val2: String): HostRole? =
            values().firstOrNull { it.name == val2 }
    }
}
