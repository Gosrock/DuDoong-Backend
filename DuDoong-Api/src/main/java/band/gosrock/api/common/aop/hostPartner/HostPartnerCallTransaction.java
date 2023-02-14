package band.gosrock.api.common.aop.hostPartner;


import org.aspectj.lang.ProceedingJoinPoint;

interface HostPartnerCallTransaction {
    Object proceed(Long id, final ProceedingJoinPoint joinPoint) throws Throwable;
}
