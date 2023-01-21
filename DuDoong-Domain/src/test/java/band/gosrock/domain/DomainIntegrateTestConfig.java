package band.gosrock.domain;


import band.gosrock.common.DuDoongCommonApplication;
import band.gosrock.domain.common.aop.domainEvent.EventPublisherAspect;
import band.gosrock.infrastructure.DuDoongInfraApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;


/**
 * 스프링 부트 설정의 컴포넌트 스캔범위를 지정
 * 통합 테스트를 위함
 */
@Configuration
@ComponentScan(basePackageClasses = {DuDoongInfraApplication.class, DuDoongDomainApplication.class,
    DuDoongCommonApplication.class})
public class DomainIntegrateTestConfig {}
