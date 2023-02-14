package band.gosrock.api.image.dto;


import band.gosrock.domain.common.vo.ImageVo;
import band.gosrock.infrastructure.config.s3.ImageUrlDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUrlResponse {

    private final String presignedUrl;
    private final String key;
    private final ImageVo url;

    public static ImageUrlResponse from(ImageUrlDto urlDto) {
        return ImageUrlResponse.builder()
                .presignedUrl(urlDto.getUrl())
                .key(urlDto.getKey())
                .url(ImageVo.valueOf(urlDto.getKey()))
                .build();
    }
}
