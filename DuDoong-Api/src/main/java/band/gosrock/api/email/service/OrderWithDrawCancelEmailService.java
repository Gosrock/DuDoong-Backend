package band.gosrock.api.email.service;


import band.gosrock.api.email.dto.OrderMailDto;
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class OrderWithDrawCancelEmailService {
    private final AwsSesUtils awsSesUtils;

    public void execute(OrderMailDto orderMailDto) {
        Context context = new Context();
        EmailUserInfo userInfo = orderMailDto.getUserInfo();
        context.setVariable("userInfo", userInfo);
        context.setVariable("orderInfo", orderMailDto.getOrderInfo());
        context.setVariable("eventInfo", orderMailDto.getEventInfo());
        awsSesUtils.singleEmailRequest(
                userInfo, "두둥 주문 철회 알림 드립니다.", "orderWithdrawCancel", context);
    }
}
