package band.gosrock.api.host.model.dto.response;

import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@Builder
public class CreateHostResponse {
    @Schema(description = "담당자 이메일")
    private final String contactEmail;

    @Schema(description = "담당자 전화번호")
    private final String contactNumber;

    @Schema(description = "마스터 유저의 고유 아이디")
    private final Long masterUserId;
//    private final HostUser masterUser;

    @Schema(description = "파트너쉽 여부")
    private final boolean partner;

    public static CreateHostResponse of(Host host) {
        return CreateHostResponse.builder()
                .contactEmail(host.getContactEmail())
                .contactNumber(host.getContactNumber())
                .masterUserId(host.getMasterUserId()) //todo:: 마스터 정보 나오도록
                .partner(host.getPartner())
                .build();
    }
}
