package band.gosrock.api.common.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HostRolesAllowed {
    /**
     * 세가지 값을 가짐 "master","manager","guest" 권한 정보는
     *
     * @see HostRoleAop
     */
    String value() default "manager";

    /** 이벤트 아이디의 식별자. */
    String eventIdIdentifier() default "eventId";
}
