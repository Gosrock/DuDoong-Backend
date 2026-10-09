package band.gosrock.infrastructure.outer.aws

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.ServerSocket
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StagingAppHealthChecker")
class StagingAppHealthCheckerTest {

    private val checker = StagingAppHealthChecker()
    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    /** 실제 HTTP 서버를 띄워 헬스체크 경로에 status 로 응답한다. "127.0.0.1:포트" 를 돌려준다 */
    private fun startServer(status: Int): String {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/api/v1/examples/health") { exchange ->
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
        }
        s.start()
        server = s
        return "127.0.0.1:${s.address.port}"
    }

    @Test
    @DisplayName("헬스체크가 200 이면 true")
    fun healthy() {
        assertTrue(checker.isHealthy(startServer(200)))
    }

    @Test
    @DisplayName("헬스체크가 502 이면 false")
    fun badGateway() {
        assertFalse(checker.isHealthy(startServer(502)))
    }

    @Test
    @DisplayName("연결이 안 되면 예외 없이 false")
    fun connectionRefused() {
        val closedPort = ServerSocket(0).use { it.localPort }
        assertFalse(checker.isHealthy("127.0.0.1:$closedPort"))
    }
}
