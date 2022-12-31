package band.gosrock.domain.user.domain;

import band.gosrock.common.consts.DuDoongConsts;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthInfo {

    @Enumerated(EnumType.STRING)
    private OauthProvider provider;

    private String oid;

    @Builder
    public OauthInfo(OauthProvider provider, String oid) {
        this.provider = provider;
        this.oid = oid;
    }

    public OauthInfo withDrawOauthInfo(){
        //TODO : 탈퇴이벤트
        return OauthInfo.builder().oid(DuDoongConsts.WITHDRAW_PREFIX + oid).provider(provider).build();
    }
}
