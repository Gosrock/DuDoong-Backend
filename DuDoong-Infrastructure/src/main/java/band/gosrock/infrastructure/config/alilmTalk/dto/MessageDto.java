package band.gosrock.infrastructure.config.alilmTalk.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class MessageDto {

    @Getter
    @Builder
    public static class AlimTalkItemButtonBody {
        private String plusFriendId;
        private String templateCode;
        private List<AlimTalkItemButtonMessage> messages;
    }

    @Getter
    @Builder
    public static class AlimTalkItemBody {
        private String plusFriendId;
        private String templateCode;
        private List<AlimTalkItemMessage> messages;
    }

    @Getter
    @Builder
    public static class AlimTalkButtonBody {
        private String plusFriendId;
        private String templateCode;
        private List<AlimTalkButtonMessage> messages;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkItemButtonMessage {
        private String to;
        private String content;
        private String headerContent;
        private AlimTalkItem item;
        private List<AlimTalkButton> buttons;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkItemMessage {
        private String to;
        private String content;
        private String headerContent;
        private AlimTalkItem item;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkButtonMessage {
        private String to;
        private String content;
        private List<AlimTalkButton> buttons;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkButton {
        private String type;
        private String name;
        private String linkMobile;
        private String linkPc;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlimTalkItem {
        private List<Item> list;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Item {
        private String title;
        private String description;
    }
}
