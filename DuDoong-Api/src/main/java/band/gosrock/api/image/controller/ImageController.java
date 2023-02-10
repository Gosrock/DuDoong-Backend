package band.gosrock.api.image.controller;


import band.gosrock.api.image.dto.ImageUrlResponse;
import band.gosrock.api.image.service.GetImageUploadUrlUseCase;
import band.gosrock.infrastructure.config.s3.ImageFileExtension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "a1. [이미지]")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ImageController {

    private final GetImageUploadUrlUseCase getImageUploadUrlUseCase;

    @Operation(summary = "이벤트 관련 이미지 업로드 url 요청할수 있는 api 입니다.")
    @PostMapping(value = "/events/{eventId}/images")
    public ImageUrlResponse getIssuedTickets(
            @PathVariable Long eventId, @RequestParam ImageFileExtension imageFileExtension) {
        return getImageUploadUrlUseCase.forEvent(eventId, imageFileExtension);
    }

    @Operation(summary = "호스트 관련 이미지 업로드 url 요청할수 있는 api 입니다.")
    @PostMapping(value = "/hosts/{hostId}/images")
    public ImageUrlResponse patchIssuedTicketStatus(
            @PathVariable Long hostId, @RequestParam ImageFileExtension imageFileExtension) {
        return getImageUploadUrlUseCase.forHost(hostId, imageFileExtension);
    }
}
