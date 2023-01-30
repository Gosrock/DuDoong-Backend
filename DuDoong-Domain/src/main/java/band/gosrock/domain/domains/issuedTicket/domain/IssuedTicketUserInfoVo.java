package band.gosrock.domain.domains.issuedTicket.domain;

import band.gosrock.domain.domains.user.domain.User;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//Todo: default constructor 고치기
@Getter
@Embeddable
@Builder
@RequiredArgsConstructor
public class IssuedTicketUserInfoVo {

    private final Long userId;

    private final String userName;

    private final String phoneNumber;

    public static IssuedTicketUserInfoVo from(User user) {
        return IssuedTicketUserInfoVo.builder().userId(user.getId())
            .userName(user.getProfile().getName()).phoneNumber(user.getProfile().getPhoneNumber())
            .build();
    }
}
