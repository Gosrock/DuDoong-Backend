package band.gosrock.domain.domains.host.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HostRole {
    // 슈퍼 호스트 (조회, 변경 가능)
    SUPER_HOST("SUPER_HOST"),
    // 일반 호스트 (조회만 가능)
    HOST("HOST");

    private final String value;

    // Enum Validation 을 위한 코드, enum 에 속하지 않으면 null 리턴
    @JsonCreator
    public static HostRole fromHostRole(String val) {
        for (HostRole hostRole : HostRole.values()) {
            if (hostRole.name().equals(val)) {
                return hostRole;
            }
        }
        return null;
    }
}
