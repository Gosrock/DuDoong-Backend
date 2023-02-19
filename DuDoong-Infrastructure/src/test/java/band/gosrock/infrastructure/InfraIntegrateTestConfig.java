package band.gosrock.infrastructure;


import band.gosrock.common.DuDoongCommonApplication;
import band.gosrock.infrastructure.DuDoongInfraApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** 스프링 부트 설정의 컴포넌트 스캔범위를 지정 통합 테스트를 위함 */
@Configuration
@ComponentScan(
        basePackageClasses = {
            DuDoongInfraApplication.class,
            DuDoongCommonApplication.class
        })
public class InfraIntegrateTestConfig {}
