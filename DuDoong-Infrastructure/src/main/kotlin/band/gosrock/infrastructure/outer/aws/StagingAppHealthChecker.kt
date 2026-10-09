package band.gosrock.infrastructure.outer.aws

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.springframework.stereotype.Component

/**
 * 스테이징 앱이 실제로 떴는지 확인한다.
 * 운영과 스테이징은 같은 VPC라 사설 IP로 nginx(80)에 붙는다. ALB·DNS를 거치지 않는다.
 */
@Component
class StagingAppHealthChecker {
    companion object {
        private const val HEALTH_PATH = "/api/v1/examples/health"
        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(3)
    }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    fun isHealthy(privateIp: String): Boolean {
        val request = HttpRequest.newBuilder(URI.create("http://$privateIp$HEALTH_PATH"))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build()
        return try {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200
        } catch (e: IOException) {
            false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }
}
