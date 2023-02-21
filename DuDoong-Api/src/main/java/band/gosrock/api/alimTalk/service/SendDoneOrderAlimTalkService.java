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
public class SendDoneOrderAlimTalkService {
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
                        + "티켓이 발급됐습니다.\n"
                        + "\n"
                        + "원활한 입장을 위해 QR코드를 미리 준비해주세요.";
        String headerContent = "주문 완료 안내";

        ncpHelper.sendItemNcpAlimTalk(
                userInfo.getPhoneNum(), "doneorder", content, headerContent, orderInfo);
    }
}
