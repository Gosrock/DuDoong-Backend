package band.gosrock.infrastructure.config.alilmTalk.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class MessageDto {

    @Getter
    @Builder
    public static class AlimTalkBody {
        private String plusFriendId;
        private String templateCode;
        private List<AlimTalkMessage> messages;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkMessage {
        private String to;
        private String content;
    }
}
