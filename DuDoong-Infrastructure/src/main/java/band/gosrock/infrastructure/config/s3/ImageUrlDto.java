package band.gosrock.infrastructure.config.s3;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUrlDto {

    private final String url;
    private final String key;
    private final String baseUrl;

    public static ImageUrlDto of(String url, String key) {
        return ImageUrlDto.builder().key(key).url(url).build();
    }
}
