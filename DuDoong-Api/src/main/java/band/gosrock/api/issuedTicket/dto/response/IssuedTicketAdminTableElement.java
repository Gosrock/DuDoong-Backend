package band.gosrock.api.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.domain.User;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssuedTicketAdminTableElement {

    @JsonUnwrapped private final IssuedTicketInfoVo issuedTicketInfo;

    private final UserInfoVo userInfo;

    private final String orderNo;

    private final List<IssuedTicketOptionAnswerVo> issuedTicketOptionAnswers;

    public static IssuedTicketAdminTableElement of(
            IssuedTicket issuedTicket, User user, Order order) {
        return IssuedTicketAdminTableElement.builder()
                .userInfo(user.toUserInfoVo())
                .orderNo(order.getOrderNo())
                .issuedTicketInfo(issuedTicket.toIssuedTicketInfoVo())
                .issuedTicketOptionAnswers(
                        issuedTicket.getIssuedTicketOptionAnswers().stream()
                                .map(IssuedTicketOptionAnswer::toIssuedTicketOptionAnswerVo)
                                .toList())
                .build();
    }
}
