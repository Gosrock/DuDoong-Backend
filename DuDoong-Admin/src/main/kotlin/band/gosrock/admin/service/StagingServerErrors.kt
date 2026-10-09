package band.gosrock.admin.service

import band.gosrock.admin.exception.StagingServerControlFailedException
import band.gosrock.infrastructure.outer.aws.StagingServerControlException
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("band.gosrock.admin.service.StagingServerErrors")

/** EC2 제어 실패를 409/503 응답으로 바꾼다. 500 + Slack 내부 오류 알림이 나가지 않게 한다 */
internal inline fun <T> withStagingErrors(action: String, block: () -> T): T =
    try {
        block()
    } catch (e: StagingServerControlException) {
        log.warn("[ADMIN-INFRA] STAGING {} FAILED - kind={}, aws={}", action, e.kind, e.message)
        throw StagingServerControlFailedException.from(e)
    }
