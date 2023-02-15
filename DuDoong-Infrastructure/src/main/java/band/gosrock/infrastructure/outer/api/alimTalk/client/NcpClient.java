package band.gosrock.infrastructure.outer.api.alimTalk.client;


import band.gosrock.infrastructure.config.AlilmTalk.dto.MessageDto;
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

    // , consumes = MediaType.APPLICATION_JSON_VALUE
    // , consumes = "application/json; charset=UTF-8"
    @PostMapping(path = "/alimtalk/v2/services/{serviceId}/messages")
    void sendAlimTalk(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader("Accept") String contentType,
            @RequestHeader("x-ncp-iam-access-key") String ncpAccessKey,
            @RequestHeader("x-ncp-apigw-timestamp") String timeStamp,
            @RequestHeader("x-ncp-apigw-signature-v2") String signature,
            @RequestBody MessageDto.AlimTalkBody alimTalkBody);

    //    class Configuration {
    //        @Bean
    //        Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> converters){
    //            return new SpringFormEncoder((new SpringFormEncoder(converters)));
    //        }
    //    }

    //    @Configuration
    //    class MultipartSupportConfig {
    //
    //        @Autowired
    //        private ObjectFactory<HttpMessageConverters> messageConverters;
    //
    //        @Bean
    //        @Primary
    //        @Scope("prototype")
    //        public Encoder feignFormEncoder() {
    //            return new SpringFormEncoder(new SpringEncoder(messageConverters));
    //        }
    //    }

}
