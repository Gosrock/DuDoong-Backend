package band.gosrock.infrastructure.outer.api.alimTalk.client;


import band.gosrock.infrastructure.config.alilmTalk.dto.MessageDto;
import band.gosrock.infrastructure.outer.api.alimTalk.config.NcpConfig;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "NcpClient",
        url = "https://sens.apigw.ntruss.com",
        configuration = NcpConfig.class)
@Headers("Content-Type: application/json; charset=UTF-8")
public interface NcpClient {

    // 주문 취소 알림톡 (아이템리스트+버튼)
    @PostMapping(
            path = "/alimtalk/v2/services/{serviceId}/messages",
            consumes = "application/json; charset=UTF-8")
    void sendItemButtonAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkItemButtonBody alimTalkItemButtonBody);

    // 주문 성공 알림톡 (아이템리스트)
    @PostMapping(
            path = "/alimtalk/v2/services/{serviceId}/messages",
            consumes = "application/json; charset=UTF-8")
    void sendItemAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkItemBody alimTalkItemBody);

    // 회원 가입 알림톡 (버튼)
    @PostMapping(
            path = "/alimtalk/v2/services/{serviceId}/messages",
            consumes = "application/json; charset=UTF-8")
    void sendButtonAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkButtonBody alimTalkButtonBody);
}
