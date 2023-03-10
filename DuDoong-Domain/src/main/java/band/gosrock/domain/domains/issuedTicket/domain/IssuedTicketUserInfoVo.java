package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.vo.PhoneNumberVo;
import band.gosrock.domain.domains.user.domain.User;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class IssuedTicketUserInfoVo {

    private Long userId;

    private String userName;

    private PhoneNumberVo phoneNumber;

    private String email;

    @Builder
    public IssuedTicketUserInfoVo(
            Long userId, String userName, String email, PhoneNumberVo phoneNumber) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public static IssuedTicketUserInfoVo from(User user) {
        return IssuedTicketUserInfoVo.builder()
                .userId(user.getId())
                .userName(user.getProfile().getName())
                .phoneNumber(user.getProfile().getPhoneNumberVo())
                .email(user.getProfile().getEmail())
                .build();
    }
}
