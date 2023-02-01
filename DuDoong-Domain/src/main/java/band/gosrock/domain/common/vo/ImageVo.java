package band.gosrock.domain.common.vo;

import static band.gosrock.common.consts.DuDoongStatic.assetDomain;

import com.fasterxml.jackson.annotation.JsonValue;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class ImageVo {

    private String imageKey;

    @JsonValue
    public String generateImageUrl() {
        // imageKey 널값 대응
        if (imageKey == null) {
            return null;
        }
        // 카카오 이미지로 회원가입한경우 대응
        if (imageKey.contains("kakao")) {
            return imageKey;
        }
        // 현재 도메인 대응
        return assetDomain + imageKey;
    }

    public ImageVo(String key) {
        this.imageKey = key;
    }

    public static ImageVo valueOf(String key) {
        return new ImageVo(key);
    }
}
