package band.gosrock.api.host.model.dto.response;


import band.gosrock.domain.domains.host.domain.Host;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostResponse {
    @Schema(description = "담당자 이메일")
    private final String contactEmail;

    @Schema(description = "담당자 전화번호")
    private final String contactNumber;

    @Schema(description = "마스터 유저의 고유 아이디")
    private final Long masterUserId;

    @Schema(description = "파트너쉽 여부")
    private final boolean partner;

    public static HostResponse of(Host host) {
        return HostResponse.builder()
                .contactEmail(host.getContactEmail())
                .contactNumber(host.getContactNumber())
                .masterUserId(host.getMasterUserId())
                .partner(host.getPartner())
                .build();
    }
}
