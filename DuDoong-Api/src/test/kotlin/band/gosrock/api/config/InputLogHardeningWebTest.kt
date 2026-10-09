package band.gosrock.api.config

import band.gosrock.api.supports.ApiIntegrateProfileResolver
import band.gosrock.api.supports.ApiIntegrateTestConfig
import band.gosrock.common.exception.TooManyRequestException
import band.gosrock.infrastructure.config.s3.ImageFileExtension
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.random.Random

/**
 * #764 요청 제한 IP 판정·presigned 발급 제한·오류 응답 문구·limit 상한 (실제 Tomcat).
 * 클라이언트 IP 판정은 Tomcat RemoteIpValve 가 하므로 MockMvc 가 아니라 실제 포트로 요청한다.
 * 테스트 클라이언트는 127.0.0.1(신뢰 프록시)에서 접속하므로 X-Forwarded-For 를 붙이면 "nginx 가 전달한 요청" 과 같다.
 * Redis 버킷은 실행 간에 남으므로 IP·유저 id 는 실행마다 새로 만든다
 */
@SpringBootTest(classes = [ApiIntegrateTestConfig::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(resolver = ApiIntegrateProfileResolver::class)
@TestPropertySource(
    properties = [
        "throttle.overdraft=3",
        "throttle.greedyRefill=3",
        "throttle.presigned-url-per-minute=2",
        "acl.whiteList=127.0.0.1,203.0.113.250,10.255.255.254",
    ],
)
@AutoConfigureMockMvc
@DisplayName("#764 입력·외부 호출·로그 보강 (웹)")
class InputLogHardeningWebTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var presignedUrlService: S3UploadPresignedUrlService

    @Autowired private lateinit var springDataWebProperties: SpringDataWebProperties

    private val http = HttpClient.newHttpClient()

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    /** 비로그인 공개 API 를 n 번 호출한 상태 코드 */
    private fun hitHealth(times: Int, vararg headers: Pair<String, String>): List<Int> = (1..times).map {
        val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/api/v2/health")).GET()
        headers.forEach { (k, v) -> request.header(k, v) }
        http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode()
    }

    /** 문서용 대역(198.18.0.0/15)에서 실행마다 다른 IP */
    private fun randomIp(): String = "198.18.${Random.nextInt(0, 256)}.${Random.nextInt(1, 255)}"

    @Test
    fun `클라이언트가 보낸 X-Forwarded-For 값은 IP 판정에 쓰지 않고 프록시가 붙인 주소로 제한한다`() {
        val statuses = hitHealth(5, "X-Forwarded-For" to "127.0.0.1, ${randomIp()}")
        assertEquals(listOf(200, 200, 200, 429, 429), statuses)
    }

    @Test
    fun `Forwarded 헤더는 클라이언트 IP 판정에 쓰지 않는다`() {
        val statuses = hitHealth(5, "Forwarded" to "for=127.0.0.1", "X-Forwarded-For" to randomIp())
        assertEquals(listOf(200, 200, 200, 429, 429), statuses)
    }

    @Test
    fun `X-Forwarded-For 는 오른쪽부터 신뢰 프록시(사설 대역 ALB)를 건너뛴 첫 주소가 클라이언트다`() {
        // 클라이언트 203.0.113.250(화이트리스트) → ALB(172.31.x) → nginx(127.0.0.1) 경로. 화이트리스트라 제한되지 않는다
        assertEquals(List(5) { 200 }, hitHealth(5, "X-Forwarded-For" to "203.0.113.250, 172.31.5.6"))
        // 화이트리스트 IP 가 클라이언트가 보낸 부분에만 있으면 쓰지 않는다
        assertEquals(listOf(200, 200, 200, 429), hitHealth(4, "X-Forwarded-For" to "203.0.113.250, ${randomIp()}, 172.31.5.6"))
    }

    @Test
    fun `nginx 변경 전 체인 - 위조값(127·사설) 뒤에 실제 IP 가 붙으면 실제 IP 로 제한한다`() {
        // 클라이언트가 "127.0.0.1, 10.0.0.5" 를 보내고 ALB 가 실제 IP, nginx 가 ALB IP 를 덧붙인 형태
        assertEquals(listOf(200, 200, 200, 429), hitHealth(4, "X-Forwarded-For" to "127.0.0.1, 10.0.0.5, ${randomIp()}, 172.31.5.6"))
        // ALB 가 없을 때(EP09): nginx 가 실제 IP 를 덧붙인 형태
        assertEquals(listOf(200, 200, 200, 429), hitHealth(4, "X-Forwarded-For" to "127.0.0.1, 10.0.0.5, ${randomIp()}"))
    }

    @Test
    fun `nginx 변경 후(Deploy #30) - 실제 IP 하나로 덮어쓴 값은 그 IP 로 판정한다 (ALB 유무 같음)`() {
        assertEquals(listOf(200, 200, 200, 429), hitHealth(4, "X-Forwarded-For" to randomIp()))
        assertEquals(List(5) { 200 }, hitHealth(5, "X-Forwarded-For" to "203.0.113.250"))
    }

    @Test
    fun `사설·루프백만 있는 체인은 화이트리스트로 통과시키지 않는다`() {
        // 모두 신뢰 프록시면 RemoteIpValve 는 맨 왼쪽 값을 쓴다 — 화이트리스트(127.0.0.1·10.255.255.254)와 같아도 제한한다.
        // Redis 버킷이 실행 간에 남으므로 처음 몇 번의 결과는 정하지 않고 429 가 나오는지만 본다
        assertTrue(429 in hitHealth(8, "X-Forwarded-For" to "127.0.0.1, 10.0.0.5"))
        assertTrue(429 in hitHealth(8, "X-Forwarded-For" to "10.255.255.254, 172.31.5.6"))
    }

    @Test
    fun `프록시 헤더 없이 루프백에서 직접 온 요청은 화이트리스트로 통과한다`() {
        assertEquals(List(5) { 200 }, hitHealth(5))
    }

    @Test
    fun `presigned URL 발급은 요청 유저별 분당 횟수로 제한한다`() {
        val userId = Random.nextLong(900_000_000, 999_000_000)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId.toString(), null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        presignedUrlService.forUser(userId, ImageFileExtension.PNG)
        presignedUrlService.forHost(1L, ImageFileExtension.PNG)
        assertThrows(TooManyRequestException::class.java) { presignedUrlService.forEvent(1L, ImageFileExtension.PNG) }

        // 다른 유저는 따로 센다
        val other = userId + 1
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(other.toString(), null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        presignedUrlService.forUser(other, ImageFileExtension.PNG)
    }

    @Test
    fun `응원톡 랜덤 limit 은 50 이하만 받고, 검증 메시지는 응답에 남는다`() {
        val body = mockMvc.get("/api/v1/events/1/comments/random") {
            with(user("1").roles("USER"))
            param("limit", "51")
        }.andExpect { status { isBadRequest() } }.andReturn().response.getContentAsString(Charsets.UTF_8)
        assertTrue(body.contains("limit 값은 50 이하여야 합니다."), body)

        val ok = mockMvc.get("/api/v1/events/1/comments/random") {
            with(user("1").roles("USER"))
            param("limit", "50")
        }.andReturn().response
        assertTrue(ok.status != 400, ok.getContentAsString(Charsets.UTF_8))
    }

    @Test
    fun `Pageable size 상한은 100`() {
        assertEquals(100, springDataWebProperties.pageable.maxPageSize)
    }

    @Test
    fun `스프링 기본 예외(타입 변환·본문 파싱)는 내부 메시지 대신 일반 문구로 응답한다`() {
        val typeMismatch = mockMvc.get("/api/v1/events/abc/comments/counts") { with(user("1").roles("USER")) }
            .andExpect { status { isBadRequest() } }.andReturn().response.getContentAsString(Charsets.UTF_8)
        val badJson = mockMvc.post("/api/v1/events/1/comments") {
            with(user("1").roles("USER"))
            contentType = MediaType.APPLICATION_JSON
            content = "{\"content\": "
        }.andExpect { status { isBadRequest() } }.andReturn().response.getContentAsString(Charsets.UTF_8)

        for (body in listOf(typeMismatch, badJson)) {
            assertEquals("Bad Request", objectMapper.readTree(body).at("/reason").asText(), body)
            assertFalse(body.contains("java.") || body.contains("JSON parse") || body.contains("Failed to convert"), body)
        }
    }
}
