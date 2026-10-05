package band.gosrock.api.supports;


import band.gosrock.DuDoongApiServerApplication;
import band.gosrock.common.DuDoongCommonApplication;
import band.gosrock.domain.DuDoongDomainApplication;
import band.gosrock.infrastructure.DuDoongInfraApplication;
import java.lang.reflect.Proxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * 스프링 부트 설정의 컴포넌트 스캔범위를 지정 통합 테스트를 위함.
 * 커넥션 측정(#743): 스레드별 커넥션 수([ThreadConnections])와 락 메서드별 측정([LockConnectionProbe])을 모든 통합 테스트에 붙인다
 */
@Configuration
@Import({LockConnectionProbe.class, LockConnectionProbePostProcessor.class})
@ComponentScan(
        basePackageClasses = {
            DuDoongInfraApplication.class,
            DuDoongDomainApplication.class,
            DuDoongCommonApplication.class,
            DuDoongApiServerApplication.class
        })
public class ApiIntegrateTestConfig {

    /**
     * 통합 테스트에서 메일(SES) 실제 호출을 막는다 (#718). 주문·환불 메일 핸들러가 비동기로 AWS 를 부르며 실패 로그를 남기던 것.
     * 모든 호출은 아무것도 하지 않고 null 을 돌려준다 (응답은 쓰지 않음). Mockito 전역 상태와 섞이지 않게 JDK 프록시로 만든다.
     * 메일 발송 결과를 검증하는 테스트는 없다
     */
    @Bean
    @Primary
    public SesClient noOpSesClient() {
        return (SesClient)
                Proxy.newProxyInstance(
                        SesClient.class.getClassLoader(),
                        new Class<?>[] {SesClient.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("toString")) return "noOpSesClient";
                            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                            if (method.getName().equals("equals")) return proxy == args[0];
                            return null;
                        });
    }
}
