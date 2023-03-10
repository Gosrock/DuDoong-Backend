package band.gosrock.api.common.aop.hostRole;


import org.aspectj.lang.ProceedingJoinPoint;

interface HostRoleCallTransaction {
    Object proceed(Long id, HostQualification role, final ProceedingJoinPoint joinPoint)
            throws Throwable;
}
