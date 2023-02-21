package band.gosrock.api.alimTalk.service;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.api.alimTalk.service.helper.NcpHelper;
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
                "안녕하세요 "
                        + userInfo.getUserName()
                        + "님!\n"
                        + eventInfo.getHostName()
                        + " "
                        + eventInfo.getEventName()
                        + " 주문이 취소되어 안내드립니다.\n";
        String headerContent = "주문 취소 안내";

        ncpHelper.sendItemButtonNcpAlimTalk(
                userInfo.getPhoneNum(), "cancelorder", content, headerContent, orderInfo);
    }
}
