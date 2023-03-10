package band.gosrock.infrastructure.config.alilmTalk;


import band.gosrock.common.annotation.Helper;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.common.helper.SpringEnvironmentHelper;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkOrderInfo;
import band.gosrock.infrastructure.config.alilmTalk.dto.MessageDto;
import band.gosrock.infrastructure.outer.api.alimTalk.client.NcpClient;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;

@Helper
public class NcpHelper {
    private final NcpClient ncpClient;
    private final String serviceID;
    private final String ncpAccessKey;
    private final String ncpSecretKey;
    private final String plusFriendId;

    private final SpringEnvironmentHelper springEnvironmentHelper;
    static String space = " "; // space
    static String newLine = "\n"; // new line
    static String method = "POST"; // method

    public NcpHelper(
            NcpClient ncpClient,
            @Value("${ncp.service-id}") String serviceID,
            @Value("${ncp.access-key}") String ncpAccessKey,
            @Value("${ncp.secret-key}") String ncpSecretKey,
            @Value("${ncp.plus-friend-id}") String plusFriendId,
            SpringEnvironmentHelper springEnvironmentHelper) {
        this.ncpClient = ncpClient;
        this.serviceID = serviceID;
        this.ncpAccessKey = ncpAccessKey;
        this.ncpSecretKey = ncpSecretKey;
        this.plusFriendId = plusFriendId;
        this.springEnvironmentHelper = springEnvironmentHelper;
    }

    // 주문 취소 알림톡 (아이템리스트+버튼)
    public void sendCancelOrderAlimTalk(
            String to,
            String templateCode,
            String content,
            String headerContent,
            AlimTalkOrderInfo orderInfo) {
        // 전송 서버 검증
        if (!springEnvironmentHelper.isProdAndStagingProfile()) {
            return;
        }
        // signature 생성
        String timeStamp =
                String.valueOf(Instant.now().toEpochMilli()); // current timestamp (epoch)
        String alimTalkSignatureRequestUrl = "/alimtalk/v2/services/" + serviceID + "/messages";
        String signature =
                makePostSignature(
                        ncpAccessKey, ncpSecretKey, alimTalkSignatureRequestUrl, timeStamp);
        // 바디 생성
        MessageDto.AlimTalkItemButtonBody alimTalkItemButtonBody =
                makeCancelOrderBody(templateCode, to, content, headerContent, orderInfo);
        ncpClient.sendItemButtonAlimTalk(
                serviceID, ncpAccessKey, timeStamp, signature, alimTalkItemButtonBody);
    }

    // 회원 가입 알림톡 (버튼)
    public void sendButtonNcpAlimTalk(String to, String templateCode, String content) {
        // 전송 서버 검증
        if (!springEnvironmentHelper.isProdAndStagingProfile()) {
            return;
        }
        // signature 생성
        String timeStamp =
                String.valueOf(Instant.now().toEpochMilli()); // current timestamp (epoch)
        String alimTalkSignatureRequestUrl = "/alimtalk/v2/services/" + serviceID + "/messages";
        String signature =
                makePostSignature(
                        ncpAccessKey, ncpSecretKey, alimTalkSignatureRequestUrl, timeStamp);
        // 바디 생성
        MessageDto.AlimTalkButtonBody body = makeButtonBody(templateCode, to, content);
        ncpClient.sendButtonAlimTalk(serviceID, ncpAccessKey, timeStamp, signature, body);
    }

    // 주문 성공 알림톡 (아이템리스트+버튼)
    public void sendDoneOrderAlimTalk(
            String to,
            String templateCode,
            String content,
            String headerContent,
            AlimTalkOrderInfo orderInfo) {
        // 전송 서버 검증
        if (!springEnvironmentHelper.isProdAndStagingProfile()) {
            return;
        }
        // signature 생성
        String timeStamp =
                String.valueOf(Instant.now().toEpochMilli()); // current timestamp (epoch)
        String alimTalkSignatureRequestUrl = "/alimtalk/v2/services/" + serviceID + "/messages";
        String signature =
                makePostSignature(
                        ncpAccessKey, ncpSecretKey, alimTalkSignatureRequestUrl, timeStamp);
        // 바디 생성
        MessageDto.AlimTalkItemButtonBody alimTalkItemButtonBody =
                makeDoneOrderBody(templateCode, to, content, headerContent, orderInfo);
        ncpClient.sendItemButtonAlimTalk(
                serviceID, ncpAccessKey, timeStamp, signature, alimTalkItemButtonBody);
    }

    // 주문서 전송 알림톡 (아이템리스트)
    public void sendSettlementNcpAlimTalk(
            String to,
            String templateCode,
            String content,
            String headerContent,
            String email,
            String eventName) {
        // 전송 서버 검증
        if (!springEnvironmentHelper.isProdAndStagingProfile()) {
            return;
        }
        // signature 생성
        String timeStamp =
                String.valueOf(Instant.now().toEpochMilli()); // current timestamp (epoch)
        String alimTalkSignatureRequestUrl = "/alimtalk/v2/services/" + serviceID + "/messages";
        String signature =
                makePostSignature(
                        ncpAccessKey, ncpSecretKey, alimTalkSignatureRequestUrl, timeStamp);
        // 바디 생성
        MessageDto.AlimTalkItemBody alimTalkItemBody =
                makeSettlementItemBody(templateCode, to, content, headerContent, email, eventName);
        ncpClient.sendItemAlimTalk(serviceID, ncpAccessKey, timeStamp, signature, alimTalkItemBody);
    }

    public MessageDto.AlimTalkItemButtonBody makeDoneOrderBody(
            String templateCode,
            String to,
            String content,
            String headerContent,
            AlimTalkOrderInfo orderInfo) {
        MessageDto.AlimTalkItem alimTalkItem = makeOrderItem(orderInfo);
        List<MessageDto.AlimTalkButton> alimTalkButtons = makeDoneOrderButtons();

        MessageDto.AlimTalkItemButtonMessage alimTalkItemButtonMessage =
                MessageDto.AlimTalkItemButtonMessage.builder()
                        .to(to)
                        .content(content)
                        .headerContent(headerContent)
                        .item(alimTalkItem)
                        .buttons(alimTalkButtons)
                        .build();

        List<MessageDto.AlimTalkItemButtonMessage> alimTalkItemButtonMessages = new ArrayList<>();
        alimTalkItemButtonMessages.add(alimTalkItemButtonMessage);

        return MessageDto.AlimTalkItemButtonBody.builder()
                .plusFriendId(plusFriendId)
                .templateCode(templateCode)
                .messages(alimTalkItemButtonMessages)
                .build();
    }

    public MessageDto.AlimTalkItemButtonBody makeCancelOrderBody(
            String templateCode,
            String to,
            String content,
            String headerContent,
            AlimTalkOrderInfo orderInfo) {
        MessageDto.AlimTalkItem alimTalkItem = makeOrderItem(orderInfo);
        List<MessageDto.AlimTalkButton> alimTalkButtons = makeCancelOrderButtons();

        MessageDto.AlimTalkItemButtonMessage alimTalkItemButtonMessage =
                MessageDto.AlimTalkItemButtonMessage.builder()
                        .to(to)
                        .content(content)
                        .headerContent(headerContent)
                        .item(alimTalkItem)
                        .buttons(alimTalkButtons)
                        .build();

        List<MessageDto.AlimTalkItemButtonMessage> alimTalkItemButtonMessages = new ArrayList<>();
        alimTalkItemButtonMessages.add(alimTalkItemButtonMessage);

        return MessageDto.AlimTalkItemButtonBody.builder()
                .plusFriendId(plusFriendId)
                .templateCode(templateCode)
                .messages(alimTalkItemButtonMessages)
                .build();
    }

    public MessageDto.AlimTalkItemBody makeSettlementItemBody(
            String templateCode,
            String to,
            String content,
            String headerContent,
            String email,
            String eventName) {
        MessageDto.AlimTalkItem alimTalkItem = makeSettlementItem(email, eventName);
        MessageDto.AlimTalkItemMessage alimTalkMessage =
                MessageDto.AlimTalkItemMessage.builder()
                        .to(to)
                        .content(content)
                        .headerContent(headerContent)
                        .item(alimTalkItem)
                        .build();
        List<MessageDto.AlimTalkItemMessage> alimTalkItemMessages = new ArrayList<>();
        alimTalkItemMessages.add(alimTalkMessage);

        return MessageDto.AlimTalkItemBody.builder()
                .plusFriendId(plusFriendId)
                .templateCode(templateCode)
                .messages(alimTalkItemMessages)
                .build();
    }

    public MessageDto.AlimTalkItemBody makeItemBody(
            String templateCode,
            String to,
            String content,
            String headerContent,
            AlimTalkOrderInfo orderInfo) {
        MessageDto.AlimTalkItem alimTalkItem = makeOrderItem(orderInfo);
        MessageDto.AlimTalkItemMessage alimTalkMessage =
                MessageDto.AlimTalkItemMessage.builder()
                        .to(to)
                        .content(content)
                        .headerContent(headerContent)
                        .item(alimTalkItem)
                        .build();
        List<MessageDto.AlimTalkItemMessage> alimTalkItemMessages = new ArrayList<>();
        alimTalkItemMessages.add(alimTalkMessage);

        return MessageDto.AlimTalkItemBody.builder()
                .plusFriendId(plusFriendId)
                .templateCode(templateCode)
                .messages(alimTalkItemMessages)
                .build();
    }

    public MessageDto.AlimTalkButtonBody makeButtonBody(
            String templateCode, String to, String content) {
        List<MessageDto.AlimTalkButton> alimTalkButtons = makeSignUpButtons();
        MessageDto.AlimTalkButtonMessage alimTalkButtonMessage =
                MessageDto.AlimTalkButtonMessage.builder()
                        .to(to)
                        .content(content)
                        .buttons(alimTalkButtons)
                        .build();

        List<MessageDto.AlimTalkButtonMessage> alimTalkButtonMessages = new ArrayList<>();
        alimTalkButtonMessages.add(alimTalkButtonMessage);

        return MessageDto.AlimTalkButtonBody.builder()
                .plusFriendId(plusFriendId)
                .templateCode(templateCode)
                .messages(alimTalkButtonMessages)
                .build();
    }

    public MessageDto.AlimTalkItem makeSettlementItem(String email, String eventName) {
        List<MessageDto.Item> list = new ArrayList<>();
        MessageDto.Item item1 = MessageDto.Item.builder().title("이메일 :").description(email).build();
        list.add(item1);
        MessageDto.Item item2 =
                MessageDto.Item.builder().title("이벤트 :").description(eventName).build();
        list.add(item2);
        return MessageDto.AlimTalkItem.builder().list(list).build();
    }

    public MessageDto.AlimTalkItem makeOrderItem(AlimTalkOrderInfo orderInfo) {
        List<MessageDto.Item> list = new ArrayList<>();
        MessageDto.Item item1 =
                MessageDto.Item.builder().title("주문명 :").description(orderInfo.getName()).build();
        list.add(item1);
        MessageDto.Item item2 =
                MessageDto.Item.builder()
                        .title("수량 :")
                        .description(orderInfo.getQuantity().toString())
                        .build();
        list.add(item2);
        MessageDto.Item item3 =
                MessageDto.Item.builder().title("가격 :").description(orderInfo.getMoney()).build();
        list.add(item3);
        MessageDto.Item item4 =
                MessageDto.Item.builder()
                        .title("주문일시 :")
                        .description(
                                orderInfo
                                        .getCreateAt()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        .build();
        list.add(item4);
        return MessageDto.AlimTalkItem.builder().list(list).build();
    }

    public MessageDto.AlimTalkButton makeHomePageButton() {
        return MessageDto.AlimTalkButton.builder()
                .type("WL")
                .name("홈페이지 바로가기")
                .linkMobile("https://dudoong.com/")
                .linkPc("https://dudoong.com/")
                .build();
    }

    public MessageDto.AlimTalkButton makeAddChannelButton() {
        return MessageDto.AlimTalkButton.builder().type("AC").name("채널 추가").build();
    }

    public MessageDto.AlimTalkButton makeMyPageButton() {
        return MessageDto.AlimTalkButton.builder()
                .type("WL")
                .name("마이페이지 바로가기")
                .linkMobile("https://dudoong.com/mypage")
                .linkPc("https://dudoong.com/mypage")
                .build();
    }

    public List<MessageDto.AlimTalkButton> makeSignUpButtons() {
        MessageDto.AlimTalkButton alimTalkButton1 = makeAddChannelButton();
        MessageDto.AlimTalkButton alimTalkButton2 = makeHomePageButton();
        List<MessageDto.AlimTalkButton> alimTalkButtons = new ArrayList<>();
        alimTalkButtons.add(alimTalkButton1);
        alimTalkButtons.add(alimTalkButton2);
        return alimTalkButtons;
    }

    public List<MessageDto.AlimTalkButton> makeDoneOrderButtons() {
        MessageDto.AlimTalkButton alimTalkButton = makeMyPageButton();
        List<MessageDto.AlimTalkButton> alimTalkButtons = new ArrayList<>();
        alimTalkButtons.add(alimTalkButton);
        return alimTalkButtons;
    }

    public List<MessageDto.AlimTalkButton> makeCancelOrderButtons() {
        MessageDto.AlimTalkButton alimTalkButton = makeHomePageButton();
        List<MessageDto.AlimTalkButton> alimTalkButtons = new ArrayList<>();
        alimTalkButtons.add(alimTalkButton);
        return alimTalkButtons;
    }

    public String makePostSignature(
            String accessKey, String secretKey, String url, String timeStamp) {
        String result;
        try {
            String message =
                    new StringBuilder()
                            .append(method)
                            .append(space)
                            .append(url)
                            .append(newLine)
                            .append(timeStamp)
                            .append(newLine)
                            .append(accessKey)
                            .toString();

            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
            String encodeBase64String = Base64.encodeBase64String(rawHmac);

            result = encodeBase64String;

        } catch (Exception ex) {
            throw new DuDoongDynamicException(0, "400", ex.getMessage());
        }
        return result;
    }
}
