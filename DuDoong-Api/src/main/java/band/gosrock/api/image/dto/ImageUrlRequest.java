package band.gosrock.api.image.dto;


import band.gosrock.common.annotation.Enum;
import band.gosrock.infrastructure.config.s3.ImageFileExtension;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUrlRequest {

    @Enum private ImageFileExtension imageFileExtension;
}
