package band.gosrock.api.common.aop.hostPartner;


import band.gosrock.api.common.aop.hostRole.FindHostFrom;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** HostPartner aop 를 적용하기 위해 다는 어노테이션 - 이찬진 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HostPartnerAllowed {
    FindHostFrom findHostFrom();
}
