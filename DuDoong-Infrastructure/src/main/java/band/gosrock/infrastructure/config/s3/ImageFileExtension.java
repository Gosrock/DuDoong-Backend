package band.gosrock.infrastructure.config.s3;


import lombok.Getter;

@Getter
public enum ImageFileExtension {
    JPEG("jpeg"),
    JPG("jpeg"),
    PNG("png");

    ImageFileExtension(String uploadExtension) {
        this.uploadExtension = uploadExtension;
    }

    private final String uploadExtension;
}
