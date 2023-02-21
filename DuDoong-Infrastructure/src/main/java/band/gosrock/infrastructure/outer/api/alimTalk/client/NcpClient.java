package band.gosrock.infrastructure.outer.api.alimTalk.client;


import band.gosrock.infrastructure.config.alilmTalk.dto.MessageDto;
import band.gosrock.infrastructure.outer.api.alimTalk.config.NcpConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "NcpClient",
        url = "https://sens.apigw.ntruss.com",
        configuration = NcpConfig.class)
public interface NcpClient {

    @PostMapping(
            path = "/alimtalk/v2/services/{serviceId}/messages",
            consumes = "application/json; charset=UTF-8")
    void sendItemButtonAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkItemButtonBody alimTalkItemButtonBody);

    @PostMapping(
            path = "/alimtalk/v2/services/{serviceId}/messages",
            consumes = "application/json; charset=UTF-8")
    void sendItemAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkItemBody alimTalkItemBody);

    @PostMapping(
            path = "/alimtalk/v2/services/{serviceId}/messages",
            consumes = "application/json; charset=UTF-8")
    void sendButtonAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkButtonBody alimTalkButtonBody);
}
