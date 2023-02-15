package band.gosrock.infrastructure.outer.api.alimTalk.config;


import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.form.spring.SpringFormEncoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.*;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

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
