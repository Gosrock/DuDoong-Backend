package band.gosrock.api.alimTalk.dto;


import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkEventInfo;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkUserInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderAlimTalkDto {
    private final AlimTalkUserInfo userInfo;
    private final AlimTalkEventInfo eventInfo;
}
