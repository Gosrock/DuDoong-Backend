package band.gosrock.api.common.aop.hostRole

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** HostRoles aop 를 적용하기 위해 다는 어노테이션 - 이찬진 */
@Target(AnnotationTarget.FUNCTION)
@Retention(RetentionPolicy.RUNTIME)
annotation class HostRolesAllowed(
    /**
     * 세가지 값을 가짐 "MASTER","MANAGER","GUEST" 권한 정보는
     *
     * @see HostRoleAop
     */
    val role: HostQualification,
    val findHostFrom: FindHostFrom,
    val applyTransaction: Boolean = true
)
