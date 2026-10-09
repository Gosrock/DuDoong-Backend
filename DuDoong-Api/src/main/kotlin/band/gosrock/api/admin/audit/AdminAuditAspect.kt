package band.gosrock.api.admin.audit

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.audit.domain.AdminAuditLog
import band.gosrock.domain.domains.audit.repository.AdminAuditLogRepository
import band.gosrock.domain.domains.audit.repository.AdminAuditSnapshotQuery
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import java.lang.reflect.Method
import java.sql.Timestamp
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping

/**
 * 운영 어드민(`/internal-api`, DuDoong-Admin 컨트롤러) 감사 기록 (#763).
 *
 * 대상: GET 이 아닌 요청(상태 변경)과 `export` 로 시작하는 메서드(엑셀 반출). 요청마다 `tbl_admin_audit_log` 1행 +
 * 구조화 로그 1줄(`[AUDIT] ADMIN …`, 로거 `AUDIT.AdminAudit`, 변경 전후 값은 넣지 않는다)을 남긴다.
 * 변경 전후 값은 경로 변수로 대상을 정해 [AdminAuditSnapshotQuery] 로 읽는다 (주문 > 티켓 > 댓글 > 호스트 멤버 > 호스트 > 공연 > 유저 순).
 * 기록 저장이 실패해도(테이블 없음 포함) 요청 처리 결과는 바꾸지 않는다 — 오류 로그만 남긴다.
 * 보관 기간 1년 (삭제 배치는 별도 작업).
 */
@Aspect
@Component
class AdminAuditAspect(
    private val adminAuditLogRepository: AdminAuditLogRepository,
    private val snapshotQuery: AdminAuditSnapshotQuery,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("within(band.gosrock.admin.controller..*) && @within(org.springframework.web.bind.annotation.RestController)")
    fun audit(joinPoint: ProceedingJoinPoint): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val method = (joinPoint.signature as MethodSignature).method
        if (request == null || !isAudited(request.method, method.name)) return joinPoint.proceed()

        val actorUserId = actorOf(method, joinPoint.args)
        val target = pathVariables(request)
        val before = snapshot(target)
        val action = "${method.declaringClass.simpleName}.${method.name}"
        val detail = requestDetail(method, joinPoint.args, request)
        try {
            val result = joinPoint.proceed()
            record(actorUserId, action, request, target, detail, before, snapshot(target), AdminAuditLog.RESULT_SUCCESS, null)
            return result
        } catch (e: Throwable) {
            record(actorUserId, action, request, target, detail, before, null, AdminAuditLog.RESULT_FAIL, errorCodeOf(e))
            throw e
        }
    }

    private fun isAudited(httpMethod: String, methodName: String): Boolean =
        httpMethod != "GET" || methodName.startsWith(EXPORT_PREFIX)

    /** @CurrentUserId 인자, 없으면 SecurityContext 의 사용자 */
    private fun actorOf(method: Method, args: Array<Any?>): Long =
        method.parameters.indices
            .firstOrNull { method.parameters[it].isAnnotationPresent(CurrentUserId::class.java) }
            ?.let { args[it] as? Long }
            ?: runCatching { SecurityUtils.getCurrentUserId() }.getOrDefault(UNKNOWN_ACTOR)

    @Suppress("UNCHECKED_CAST")
    private fun pathVariables(request: HttpServletRequest): Map<String, String> =
        (request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<String, String>).orEmpty()

    /**
     * 본문(@RequestBody), 없으면 요청 파라미터(엑셀 필터)를 JSON 으로. 개인정보를 남기지 않도록
     * [DETAIL_ALLOWED_KEYS](id·상태·역할·수량·날짜 등)만 값을 남기고 나머지(이름·연락처·사유·검색어·소개 등)는 `***(len=N)` 로 길이만 남긴다
     */
    private fun requestDetail(method: Method, args: Array<Any?>, request: HttpServletRequest): String? {
        val body = method.parameters.indices
            .firstOrNull { method.parameters[it].isAnnotationPresent(RequestBody::class.java) }
            ?.let { args[it] }
        val fields: Map<String, Any?> = if (body != null) {
            runCatching { objectMapper.convertValue(body, Map::class.java) }.getOrNull()
                ?.entries?.associate { (k, v) -> k.toString() to v } ?: return null
        } else {
            request.parameterMap.mapValues { (_, v) -> if (v.size == 1) v[0] else v.toList() }
        }
        if (fields.isEmpty()) return null
        return toJson(fields.mapValues { (key, value) -> maskUnlessAllowed(key, value) })
    }

    private fun maskUnlessAllowed(key: String, value: Any?): Any? =
        if (value == null || key in DETAIL_ALLOWED_KEYS) value else "***(len=${value.toString().length})"

    private fun snapshot(target: Map<String, String>): String? = try {
        val values = target["orderUuid"]?.let { snapshotQuery.order(it) }
            ?: target["ticketItemId"]?.toLongOrNull()?.let { snapshotQuery.ticketItem(it) }
            ?: target["commentId"]?.toLongOrNull()?.let { snapshotQuery.comment(it) }
            ?: hostMemberSnapshot(target)
            ?: target["hostId"]?.toLongOrNull()?.let { snapshotQuery.host(it) }
            ?: target["eventId"]?.toLongOrNull()?.let { snapshotQuery.event(it) }
            ?: target["userId"]?.toLongOrNull()?.let { snapshotQuery.user(it) }
        values?.let { toJson(it.mapValues { (_, v) -> if (v is Timestamp) v.toLocalDateTime().toString() else v }) }
    } catch (e: Exception) {
        log.warn("[AUDIT] 감사 대상 값 조회 실패 - target={}, error={}", target, e.javaClass.simpleName)
        null
    }

    private fun hostMemberSnapshot(target: Map<String, String>): Map<String, Any?>? {
        val hostId = target["hostId"]?.toLongOrNull() ?: return null
        val userId = target["targetUserId"]?.toLongOrNull() ?: return null
        return snapshotQuery.hostMember(hostId, userId)
    }

    private fun record(
        actorUserId: Long,
        action: String,
        request: HttpServletRequest,
        target: Map<String, String>,
        detail: String?,
        before: String?,
        after: String?,
        result: String,
        errorCode: String?,
    ) {
        val targetJson = target.takeIf { it.isNotEmpty() }?.let(::toJson)
        auditLog.info(
            "[AUDIT] ADMIN action={} actor={} method={} path={} target={} result={} errorCode={}",
            action, actorUserId, request.method, request.requestURI, targetJson, result, errorCode,
        )
        try {
            adminAuditLogRepository.save(
                AdminAuditLog(
                    actorUserId = actorUserId,
                    action = action.take(100),
                    httpMethod = request.method,
                    requestPath = request.requestURI.take(255),
                    target = targetJson?.take(255),
                    requestDetail = detail?.take(AdminAuditLog.MAX_VALUE_LENGTH),
                    beforeValue = before?.take(AdminAuditLog.MAX_VALUE_LENGTH),
                    afterValue = after?.take(AdminAuditLog.MAX_VALUE_LENGTH),
                    result = result,
                    errorCode = errorCode?.take(100),
                )
            )
        } catch (e: Exception) {
            log.error("[AUDIT] 감사 기록 저장 실패 - action={}, actor={}, error={}", action, actorUserId, e.javaClass.simpleName)
        }
    }

    private fun errorCodeOf(e: Throwable): String =
        (e as? DuDoongCodeException)?.errorCode?.getErrorReason()?.code ?: e.javaClass.simpleName

    private fun toJson(value: Any): String? = try {
        objectMapper.writeValueAsString(value)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val EXPORT_PREFIX = "export"
        private const val UNKNOWN_ACTOR = 0L

        /** request_detail 에 값을 그대로 남기는 키 (개인정보가 아닌 id·상태·역할·수량·날짜). 그 밖의 키는 길이만 */
        val DETAIL_ALLOWED_KEYS = setOf(
            "status", "role", "refundStatus", "partner", "userId", "newMasterUserId", "eventId",
            "delta", "quantity", "purchaseLimit", "price", "runTime", "startAt", "startDate", "endDate", "jobName",
        )
        private val auditLog = LoggerFactory.getLogger("AUDIT.AdminAudit")
    }
}
