package band.gosrock.domain.domains.audit.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 운영 어드민(`/internal-api`) 감사 기록 (#763, V011). 상태 변경 요청과 엑셀 반출마다 1행. 쓰기만 하고 고치지 않는다.
 *
 * - actor_user_id: 요청한 관리자. action: `컨트롤러.메서드`
 * - target: 경로 변수(대상 id) JSON
 * - request_detail: 요청 본문 또는 요청 파라미터(엑셀 필터) JSON. 허용 키(id·상태·역할·수량·날짜)만 값을 남기고
 *   그 밖(이름·연락처·사유·검색어·소개 등)은 `***(len=N)` 로 길이만 남긴다
 * - before_value·after_value: 대상의 핵심 값(상태·역할·재고 등, 사용자 이름·연락처 제외) JSON. 요청 전후에 JDBC 로 읽는다. 대상이 없으면 null
 * - 보관 기간 1년 (삭제 배치는 별도 작업)
 * - result: SUCCESS / FAIL. error_code: 실패한 경우의 오류 코드(또는 예외 이름)
 * - 긴 값은 [MAX_VALUE_LENGTH] 자에서 자른다
 */
@Table(
    name = "tbl_admin_audit_log",
    indexes = [
        Index(name = "idx_admin_audit_log_actor_user_id", columnList = "actor_user_id, admin_audit_log_id"),
        Index(name = "idx_admin_audit_log_created_at", columnList = "created_at"),
    ],
)
@Entity
class AdminAuditLog(
    @Column(name = "actor_user_id", nullable = false)
    val actorUserId: Long,
    @Column(name = "action", nullable = false, length = 100)
    val action: String,
    @Column(name = "http_method", nullable = false, length = 10)
    val httpMethod: String,
    @Column(name = "request_path", nullable = false, length = 255)
    val requestPath: String,
    @Column(name = "target", length = 255)
    val target: String?,
    @Column(name = "request_detail", length = MAX_VALUE_LENGTH)
    val requestDetail: String?,
    @Column(name = "before_value", length = MAX_VALUE_LENGTH)
    val beforeValue: String?,
    @Column(name = "after_value", length = MAX_VALUE_LENGTH)
    val afterValue: String?,
    @Column(name = "result", nullable = false, length = 20)
    val result: String,
    @Column(name = "error_code", length = 100)
    val errorCode: String?,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_audit_log_id")
    var id: Long? = null
        protected set

    companion object {
        const val MAX_VALUE_LENGTH = 2000
        const val RESULT_SUCCESS = "SUCCESS"
        const val RESULT_FAIL = "FAIL"
    }
}
