package band.gosrock.api.host.controller;


import band.gosrock.api.host.model.dto.request.CreateHostRequest;
import band.gosrock.api.host.model.dto.response.CreateHostResponse;
import band.gosrock.api.host.model.dto.response.HostResponse;
import band.gosrock.api.host.service.CreateHostUseCase;
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

    private final CreateHostUseCase createHostUseCase;
    private final ReadHostUseCase readHostUseCase;

    @Operation(summary = "내가 속한 호스트 리스트를 가져옵니다")
    @GetMapping
    public List<HostResponse> getAllHosts() {
        return readHostUseCase.execute();
    }

    @Operation(summary = "새로운 이벤트(공연)를 생성합니다")
    @PostMapping
    public CreateHostResponse createEvent(
            @RequestBody @Valid CreateHostRequest createEventRequest) {
        return createHostUseCase.execute(createEventRequest);
    }
}
