package band.gosrock.api.alimTalk.service;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.domain.common.alarm.OrderKakaoTalkAlarm;
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkEventInfo;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkOrderInfo;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendWithdrawOrderAlimTalkService {
    private final NcpHelper ncpHelper;

    public void execute(OrderAlimTalkDto orderAlimTalkDto) {
        AlimTalkUserInfo userInfo = orderAlimTalkDto.getUserInfo();
        AlimTalkEventInfo eventInfo = orderAlimTalkDto.getEventInfo();
        AlimTalkOrderInfo orderInfo = orderAlimTalkDto.getOrderInfo();

        String content =
                OrderKakaoTalkAlarm.deletionOf(
                        userInfo.getUserName(), eventInfo.getHostName(), eventInfo.getEventName());
        String headerContent = OrderKakaoTalkAlarm.deletionHeaderOf();

        ncpHelper.sendCancelOrderAlimTalk(
                userInfo.getPhoneNum(),
                OrderKakaoTalkAlarm.deletionTemplateCode(),
                content,
                headerContent,
                orderInfo);
    }
}
