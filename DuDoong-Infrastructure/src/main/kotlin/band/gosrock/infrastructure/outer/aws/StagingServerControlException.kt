package band.gosrock.infrastructure.outer.aws

/**
 * 스테이징 EC2 제어 실패. 호출 측(어드민)이 사용자 응답으로 바꾼다 (500 + Slack 내부 오류 알림 대신).
 * - STATE_CONFLICT: 인스턴스가 상태를 바꾸는 중이라 요청을 받을 수 없음 (예: 끄는 중에 켜기)
 * - UNAVAILABLE: 권한·네트워크·AWS 장애 등으로 호출 실패
 */
class StagingServerControlException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    enum class Kind { STATE_CONFLICT, UNAVAILABLE }
}
