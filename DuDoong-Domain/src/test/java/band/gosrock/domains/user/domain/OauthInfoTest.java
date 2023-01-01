package band.gosrock.domains.user.domain;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.common.consts.DuDoongStatic;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import org.junit.jupiter.api.Test;

class OauthInfoTest {

    @Test
    public void 탈퇴시에_OauthInfo_oid가_탈퇴상태가되어야한다() {
        // given
        String testOid = "test";
        String withDrawOid = DuDoongStatic.WITHDRAW_PREFIX + testOid;
        OauthInfo oauthInfo =
                OauthInfo.builder().oid(testOid).provider(OauthProvider.KAKAO).build();

        // when
        OauthInfo withDrawOauthInfo = oauthInfo.withDrawOauthInfo();

        // then
        assertEquals(withDrawOid, withDrawOauthInfo.getOid());
    }
}
