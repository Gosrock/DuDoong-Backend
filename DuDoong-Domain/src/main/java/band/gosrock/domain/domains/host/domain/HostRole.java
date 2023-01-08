package band.gosrock.domain.domains.host.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HostRole {
    // 슈퍼 호스트
    SUPER_HOST("SUPER_HOST"),
    // 일반 호스트
    HOST("HOST");

    private String value;
}
