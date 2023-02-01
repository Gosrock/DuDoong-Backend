package band.gosrock.domain.common.vo;

import static band.gosrock.common.consts.DuDoongStatic.assetDomain;

import com.fasterxml.jackson.annotation.JsonValue;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Embeddable
public class ImageVo {

    private String key;

    @JsonValue
    public String url(){
        return assetDomain + key;
    }
}
