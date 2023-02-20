package band.gosrock.api.alimTalk.service;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.api.alimTalk.service.helper.NcpHelper;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkEventInfo;
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

        String content =
                "안녕하세요 "
                        + userInfo.getUserName()
                        + "님!\n"
                        + eventInfo.getHostName()
                        + " "
                        + eventInfo.getEventName()
                        + "티켓이 발급됐습니다.\n"
                        + "\n"
                        + "원활한 입장을 위해 QR코드를 미리 준비해주세요.\n"
                        + "마이페이지 -> 내 에매 내역 -> 예매 확인 -> QR 코드 보기에서 각 티켓의 QR코드를 확인할 수 있습니다.";

        ncpHelper.sendNcpAlimTalk(userInfo.getPhoneNum(), "ticketdev", content);
    }
}
