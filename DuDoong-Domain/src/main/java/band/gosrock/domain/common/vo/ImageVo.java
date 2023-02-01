package band.gosrock.domain.common.vo;

import static band.gosrock.common.consts.DuDoongStatic.assetDomain;

import com.fasterxml.jackson.annotation.JsonValue;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class ImageVo {

    private String imageKey;

    @JsonValue
    public String url(){
        if(imageKey == null){
            return null;
        }
        if(imageKey.contains("kakao")){
            return imageKey;
        }
        return assetDomain + imageKey;
    }

    public ImageVo(String key) {
        this.imageKey = key;
    }

    public static ImageVo valueOf(String key){
        return new ImageVo(key);
    };
}
