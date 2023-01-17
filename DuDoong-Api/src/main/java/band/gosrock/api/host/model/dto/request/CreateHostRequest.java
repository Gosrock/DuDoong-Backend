package band.gosrock.api.host.model.dto.request;

import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@RequiredArgsConstructor
public class CreateHostRequest {
    @Schema(defaultValue = "gosrock@gsrk.com", description = "마스터 이메일")
    @NotNull(message = "담당자 이메일을 입력하세요")
    private final String contactEmail;

    @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
    @NotNull(message = "담당자 번호를 입력하세요")
    //todo:: 정규식 적용
    private final String contactNumber;

//    private final Long masterUserId;

    private final boolean partner;

    public Host toEntity(Long masterUserId) {
        return Host.builder()
                .contactEmail(contactEmail)
                .contactNumber(contactNumber)
                .masterUserId(masterUserId)
                .partner(partner)
                .build();
    }
}
