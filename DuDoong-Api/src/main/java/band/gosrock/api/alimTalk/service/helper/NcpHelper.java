package band.gosrock.api.alimTalk.service.helper;


import band.gosrock.common.annotation.Helper;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.infrastructure.config.AlilmTalk.dto.MessageDto;
import band.gosrock.infrastructure.outer.api.alimTalk.client.NcpClient;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

@Helper
public class NcpHelper {
    private final NcpClient ncpClient;
    private final String serviceID;
    private final String ncpAccessKey;
    private final String ncpSecretKey;
    private final String plusFriendId;

    public NcpHelper(
            NcpClient ncpClient,
            @Value("${ncp.service-id}") String serviceID,
            @Value("${ncp.access-key}") String ncpAccessKey,
            @Value("${ncp.secret-key}") String ncpSecretKey,
            @Value("${ncp.plus-friend-id}") String plusFriendId) {
        this.ncpClient = ncpClient;
        this.serviceID = serviceID;
        this.ncpAccessKey = ncpAccessKey;
        this.ncpSecretKey = ncpSecretKey;
        this.plusFriendId = plusFriendId;
    }

    public void sendNcpAlimTalk(String to, String templateCode, String content) {
        // signature 생성
        String alimTalkSignatureRequestUrl = "/alimtalk/v2/services/" + serviceID + "/messages";
        String[] signatureArray =
                makePostSignature(ncpAccessKey, ncpSecretKey, alimTalkSignatureRequestUrl);
        // 헤더 생성
        //Map<String, String> headerMap = makeHeader(ncpAccessKey, signatureArray);
        // 바디 생성
        //Map<String,Object> body= makeBodyMap(templateCode,to,content);
        MessageDto.AlimTalkBody body = makeBody(templateCode, to, content);
        //byte[] body=makeBodyEntity(templateCode,to,content);
        // api 전송
        //"application/json; charset=UTF-8"
        ncpClient.sendAlimTalk(serviceID, "application/json; charset=UTF-8", ncpAccessKey,signatureArray[0],signatureArray[1], body);
        //ncpClient.sendAlimTalk(serviceID, headerMap, body);
    }

    public MessageDto.AlimTalkBody makeBody(String templateCode, String to, String content) {
        MessageDto.AlimTalkMessage alimTalkMessage=
                MessageDto.AlimTalkMessage.builder()
                        .to(to)
                        .content(content)
                        .build();
        List<MessageDto.AlimTalkMessage> alimTalkMessages = new ArrayList<>();
        alimTalkMessages.add(alimTalkMessage);
        
        return MessageDto.AlimTalkBody.builder()
                .plusFriendId(plusFriendId)
                .templateCode(templateCode)
                .messages(alimTalkMessages)
                .build();
    }

    public Map<String, String> makeHeader(String ncpAccessKey, String[] signatureArray) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json; charset=UTF-8");
        headerMap.put("x-ncp-iam-access-key", ncpAccessKey);
        headerMap.put("x-ncp-apigw-timestamp", signatureArray[0]);
        headerMap.put("x-ncp-apigw-signature-v2", signatureArray[1]);
        return headerMap;
    }

    public String[] makePostSignature(String accessKey, String secretKey, String url) {
        String[] result = new String[2];
        try {

            String timeStamp =
                    String.valueOf(Instant.now().toEpochMilli()); // current timestamp (epoch)
            String space = " "; // space
            String newLine = "\n"; // new line
            String method = "POST"; // method

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

            result[0] = timeStamp;
            result[1] = encodeBase64String;

        } catch (Exception ex) {
            throw new DuDoongDynamicException(0, "400", ex.getMessage());
        }
        return result;
    }

    public byte[] makeBodyEntity(String templateCode, String to, String content)
            throws JSONException, UnsupportedEncodingException {
        JSONObject msgObj = new JSONObject();
        msgObj.put("plusFriendId", plusFriendId);
        msgObj.put("templateCode", templateCode);

        JSONObject messages = new JSONObject();
        messages.put("to", to);
        messages.put("content", content);

        JSONArray messageArray = new JSONArray();
        messageArray.put(messages);
        msgObj.put("messages", messageArray);

        //StringEntity body= new StringEntity(msgObj.toString(), "UTF-8");
        return  msgObj.toString().getBytes();
    }

    public Map<String,Object> makeBodyMap(String templateCode, String to, String content)
            throws JSONException {
        JSONObject msgObj = new JSONObject();
        msgObj.put("plusFriendId", plusFriendId);
        msgObj.put("templateCode", templateCode);

        JSONObject messages = new JSONObject();
        messages.put("to", to);
        messages.put("content", content);

        JSONArray messageArray = new JSONArray();
        messageArray.put(messages);
        msgObj.put("messages", messageArray);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("messages",msgObj);
        //StringEntity body= new StringEntity(msgObj.toString(), "UTF-8");
        return bodyMap;
    }
}
