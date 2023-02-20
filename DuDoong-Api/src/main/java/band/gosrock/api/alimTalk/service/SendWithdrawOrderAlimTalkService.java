package band.gosrock.api.alimTalk.service;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.api.alimTalk.service.helper.NcpHelper;
import band.gosrock.infrastructure.config.AlilmTalk.dto.AlimTalkEventInfo;
import band.gosrock.infrastructure.config.AlilmTalk.dto.AlimTalkUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendWithdrawOrderAlimTalkService {
    private final NcpHelper ncpHelper;

    public void execute(OrderAlimTalkDto orderAlimTalkDto) {
        AlimTalkUserInfo userInfo = orderAlimTalkDto.getUserInfo();
        AlimTalkEventInfo eventInfo = orderAlimTalkDto.getEventInfo();

        String content =
                "안녕하세요 "
                        + userInfo.getUserName()
                        + "님!\n"
                        + eventInfo.getHostName()
                        + " "
                        + eventInfo.getEventName()
                        + " 주문이 취소되어 안내드립니다.\n"
                        + "\n"
                        + "취소 금액에 대한 입금은 카드사 영업일 기준 4-5일이 소요될 수 있습니다. ";

        ncpHelper.sendNcpAlimTalk(userInfo.getPhoneNum(), "cancelorderdev", content);
    }
}
