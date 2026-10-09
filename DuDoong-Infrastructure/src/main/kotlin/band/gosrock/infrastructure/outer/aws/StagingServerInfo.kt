package band.gosrock.infrastructure.outer.aws

import java.time.Instant

data class StagingServerInfo(
    val state: StagingServerState,
    val launchTime: Instant?,
)
