package band.gosrock.infrastructure.outer.aws

enum class StagingServerState {
    STOPPED,
    PENDING,
    RUNNING,
    STOPPING,
    NOT_CONFIGURED,
    UNKNOWN,
    ;

    companion object {
        /** EC2 인스턴스 상태 이름(pending, running ...)을 스테이징 서버 상태로 변환한다. */
        fun fromEc2StateName(stateName: String?): StagingServerState =
            when (stateName) {
                "pending" -> PENDING
                "running" -> RUNNING
                "stopping", "shutting-down" -> STOPPING
                "stopped" -> STOPPED
                else -> UNKNOWN
            }
    }
}
