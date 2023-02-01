package band.gosrock.api.common.aop.hostRole;


import org.aspectj.lang.ProceedingJoinPoint;

public interface HostRoleCallTransaction {
    Object proceed(Long id, HostQualification role, final ProceedingJoinPoint joinPoint)
            throws Throwable;
}
