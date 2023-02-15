package band.gosrock.infrastructure.config.AlilmTalk.dto;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.entity.StringEntity;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.List;

public class MessageDto {

    @Getter
    @Builder
    public static class AlimTalkBody{
        private String plusFriendId;
        private String templateCode;
        private List<AlimTalkMessage> messages;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkMessage{
        private String to;
        private String content;
    }
//    public MessageDto(Object body) {
//        this.messages = body;
//    }
//
//    public static MessageDto from(Object messages) {
//        return new MessageDto(messages);
//    }
}
