package band.gosrock.infrastructure.outer.aws

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("StagingServerClient")
class StagingServerClientTest {

    @ParameterizedTest
    @CsvSource(
        "pending, PENDING",
        "running, RUNNING",
        "stopping, STOPPING",
        "shutting-down, STOPPING",
        "stopped, STOPPED",
        "terminated, UNKNOWN",
        "unknown-state, UNKNOWN",
    )
    @DisplayName("EC2 상태 이름을 스테이징 서버 상태로 변환한다")
    fun mapsEc2StateName(stateName: String, expected: StagingServerState) {
        assertEquals(expected, StagingServerState.fromEc2StateName(stateName))
    }

    @Test
    @DisplayName("EC2 상태 이름이 없으면 UNKNOWN")
    fun nullStateName() {
        assertEquals(StagingServerState.UNKNOWN, StagingServerState.fromEc2StateName(null))
    }

    @Test
    @DisplayName("인스턴스 ID 가 비어 있으면 AWS 호출 없이 NOT_CONFIGURED")
    fun notConfigured() {
        val client = StagingServerClient("")

        assertFalse(client.isConfigured())
        val info = client.describe()
        assertEquals(StagingServerState.NOT_CONFIGURED, info.state)
        assertNull(info.launchTime)
    }

    @Test
    @DisplayName("인스턴스 ID 가 있으면 설정된 것으로 본다")
    fun configured() {
        assertTrue(StagingServerClient("i-0123456789abcdef0").isConfigured())
    }
}
