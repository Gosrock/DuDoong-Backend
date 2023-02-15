package band.gosrock.infrastructure.outer.api.alimTalk.config;


import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

@Import(NcpErrorDecoder.class)
public class NcpConfig {
    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public NcpErrorDecoder commonFeignErrorDecoder() {
        return new NcpErrorDecoder();
    }

    //    @Bean
    //    public Encoder feignFormEncoder () {
    //        return new SpringFormEncoder(new JacksonEncoder());
    //    }
    //
    //    @Bean
    //    public FeignFormatterRegistrar localDateFeignFormatterRegistrar() {
    //        return formatterRegistry -> {
    //            DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
    //            registrar.setUseIsoFormat(true);
    //            registrar.registerFormatters(formatterRegistry);
    //        };
    //    }

    //    @Configuration
    //    class MultipartSupportConfig {
    //
    //        @Autowired
    //        private ObjectFactory<HttpMessageConverters> messageConverters;
    //
    //        @Bean
    //        public Encoder feignFormEncoder() {
    //            return new SpringFormEncoder(new SpringEncoder(messageConverters));
    //        }
    //    }
}
