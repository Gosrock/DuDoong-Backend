package band.gosrock.infrastructure.config.AlilmTalk;


import band.gosrock.common.exception.DuDoongDynamicException;
import java.io.IOException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class AlimTalk {
    private final String serviceID;
    private final String ncpAccessKey;
    private final String ncpSecretKey;
    private final String plusFriendId;

    public AlimTalk(
            @Value("${ncp.service-id}") String serviceID,
            @Value("${ncp.access-key}") String ncpAccessKey,
            @Value("${ncp.secret-key}") String ncpSecretKey,
            @Value("${ncp.plus-friend-id}") String plusFriendId) {
        this.serviceID = serviceID;
        this.ncpAccessKey = ncpAccessKey;
        this.ncpSecretKey = ncpSecretKey;
        this.plusFriendId = plusFriendId;
    }

    public void sendAlimTalk(String to, String templateCode, String content) {
        String alimTalkSendRequestUrl =
                "https://sens.apigw.ntruss.com/alimtalk/v2/services/" + serviceID + "/messages";
        String alimTalkSignatureRequestUrl = "/alimtalk/v2/services/" + serviceID + "/messages";
        CloseableHttpClient httpClient = null;
        try {
            // signature 생성
            String[] signatureArray =
                    makePostSignature(ncpAccessKey, ncpSecretKey, alimTalkSignatureRequestUrl);

            // http 통신 객체 생성
            httpClient = HttpClients.createDefault(); // http client 생성
            HttpPost httpPost = new HttpPost(alimTalkSendRequestUrl); // post 메서드와 URL 설정

            // 헤더 설정
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
            httpPost.setHeader("x-ncp-iam-access-key", ncpAccessKey);
            httpPost.setHeader("x-ncp-apigw-timestamp", signatureArray[0]);
            httpPost.setHeader("x-ncp-apigw-signature-v2", signatureArray[1]);

            // body 설정
            JSONObject msgObj = new JSONObject();
            msgObj.put("plusFriendId", plusFriendId);
            msgObj.put("templateCode", templateCode);

            JSONObject messages = new JSONObject();
            messages.put("to", "1065815885");
            messages.put("content", content);

            JSONArray messageArray = new JSONArray();
            messageArray.put(messages);
            msgObj.put("messages", messageArray);

            // api 전송 값 http 객체에 담기
            httpPost.setEntity(new StringEntity(msgObj.toString(), "UTF-8"));
            // api 호출
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            // 응답 결과
            String result = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            System.out.println(result);

        } catch (Exception ex) {
            throw new DuDoongDynamicException(0, "400", ex.getMessage());

        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new DuDoongDynamicException(0, "400", e.getMessage());
            }
        }
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

            System.out.println(result[0]);
            System.out.println(result[1]);

        } catch (Exception ex) {
            throw new DuDoongDynamicException(0, "400", ex.getMessage());
        }
        return result;
    }

    public String[] makeGetSignature(String accessKey, String secretKey, String url) {
        String[] result = new String[2];
        try {
            String timeStamp =
                    String.valueOf(Instant.now().toEpochMilli()); // current timestamp (epoch)
            String space = " "; // space
            String newLine = "\n"; // new line
            String method = "GET"; // method

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
}
