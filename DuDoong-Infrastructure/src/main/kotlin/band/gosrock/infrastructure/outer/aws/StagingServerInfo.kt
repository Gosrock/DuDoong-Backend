package band.gosrock.infrastructure.outer.aws

import java.time.Instant

data class StagingServerInfo(
    val state: StagingServerState,
    val launchTime: Instant?,
    /** VPC 사설 IP. 껐다 켜도 바뀌지 않아 앱 헬스체크(내부 통신)에 쓴다 */
    val privateIp: String? = null,
)
