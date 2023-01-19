package band.gosrock.api.host.controller;


import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.HostDetailResponse;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.api.host.service.CreateHostUseCase;
import band.gosrock.api.host.service.JoinHostUseCase;
import band.gosrock.api.host.service.ReadHostListUseCase;
import band.gosrock.api.host.service.ReadHostUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "호스트 관련 컨트롤러")
@RestController
@RequestMapping("/v1/hosts")
@RequiredArgsConstructor
public class HostController {

    private final ReadHostUseCase readHostUseCase;
    private final ReadHostListUseCase readHostListUseCase;
    private final CreateHostUseCase createHostUseCase;
    private final JoinHostUseCase joinHostUseCase;

    @Operation(summary = "내가 속한 호스트 리스트를 가져옵니다.")
    @GetMapping
    public List<HostResponse> getAllHosts() {
        return readHostListUseCase.execute();
    }

    @Operation(summary = "새로운 호스트를 생성합니다. 호스트를 생성한 유저 자신은 슈퍼호스트가 됩니다.")
    @PostMapping
    public HostResponse createHost(@RequestBody @Valid CreateHostRequest createEventRequest) {
        return createHostUseCase.execute(createEventRequest);
    }

    @Operation(summary = "내가 속해있는, 고유 아이디에 해당하는 호스트 정보를 가져옵니다.")
    @GetMapping("/{hostId}")
    public HostDetailResponse getHostById(@PathVariable Long hostId) {
        return readHostUseCase.execute(hostId);
    }

    @Operation(summary = "기존 호스트에 가입합니다.")
    @PostMapping("/{hostId}/join")
    public HostDetailResponse joinHost(@PathVariable Long hostId) {
        return joinHostUseCase.execute(hostId);
    }
}
