package band.gosrock.api.auth

import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.api.auth.service.helper.OauthStateHelper
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.api.auth.model.dto.KakaoUserInfoDto
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.ticket.V2TicketApiTestSupport
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.domain.domains.audit.domain.AdminAuditLog
import band.gosrock.domain.domains.audit.repository.AdminAuditLogRepository
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoTokenResponse
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.http.Cookie
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * #763 토큰 전달·쿠키·CORS·감사 기록 통합 테스트.
 * 카카오 호출(id_token 검증·사용자 정보·코드 교환)만 [SpyBean] 으로 바꾸고 나머지는 실제 필터 체인·유스케이스를 쓴다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["auth.origin-check.enforce=true"]) // 차단 모드. report-only(기본값)는 CookieOriginFilterTest
@DisplayName("#763 토큰·쿠키·CORS·감사")
class TokenCookieCorsIntegrationTest : V2TicketApiTestSupport() {

    @SpyBean private lateinit var kakaoOauthHelper: KakaoOauthHelper

    @Autowired private lateinit var tokenGenerateHelper: TokenGenerateHelper

    @Autowired private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired private lateinit var adminAuditLogRepository: AdminAuditLogRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private fun ResultActionsDsl.code(): String? = body().at("/code").asText().ifEmpty { null }

    private fun ResultActionsDsl.setCookies(): List<String> = andReturn().response.getHeaders("Set-Cookie")

    @Nested
    @DisplayName("1. 쿼리스트링 토큰 → 본문·쿠키 (전환 기간 동안 쿼리도 받음)")
    inner class QueryTokens {

        @Test
        fun `refresh - 본문, 쿼리, 쿠키 모두 된다`() {
            val u = newUser()
            val first = tokenGenerateHelper.execute(u)

            val byBody = mockMvc.post("/api/v1/auth/token/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("refreshToken" to first.refreshToken))
            }.andExpect { status { isOk() } }.data()
            assertEquals(u.id, byBody.at("/userProfile/id").asLong())

            val byQuery = mockMvc.post("/api/v1/auth/token/refresh") { param("token", byBody.at("/refreshToken").asText()) }
                .andExpect { status { isOk() } }.data()

            mockMvc.post("/api/v1/auth/token/refresh") { cookie(Cookie("refreshToken", byQuery.at("/refreshToken").asText())) }
                .andExpect { status { isOk() } }
        }

        @Test
        fun `쿼리 방식 호환 - form·text plain Content-Type 에 빈 본문이어도 415 가 나지 않는다`() {
            val first = tokenGenerateHelper.execute(newUser())
            val byForm = mockMvc.post("/api/v1/auth/token/refresh") {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
                param("token", first.refreshToken)
            }.andExpect { status { isOk() } }.data()
            mockMvc.post("/api/v1/auth/token/refresh?token=${byForm.at("/refreshToken").asText()}") { contentType = MediaType.TEXT_PLAIN }
                .andExpect { status { isOk() } }

            val u = newUser()
            doReturn(u.oauthInfo).`when`(kakaoOauthHelper).getOauthInfoByIdToken("form-$u")
            mockMvc.post("/api/v1/auth/oauth/kakao/login?id_token=form-$u") { contentType = MediaType.APPLICATION_FORM_URLENCODED }
                .andExpect { status { isOk() } }
        }

        @Test
        fun `깨진 JSON 본문은 400`() {
            mockMvc.post("/api/v1/auth/token/refresh") { contentType = MediaType.APPLICATION_JSON; content = "{not json" }
                .andExpect { status { isBadRequest() } }
        }

        @Test
        fun `login - 본문 idToken, 쿼리 id_token 모두 되고 둘 다 없으면 400`() {
            val u = newUser()
            doReturn(u.oauthInfo).`when`(kakaoOauthHelper).getOauthInfoByIdToken("id-token-$u")

            mockMvc.post("/api/v1/auth/oauth/kakao/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("idToken" to "id-token-$u"))
            }.andExpect { status { isOk() } }
            mockMvc.post("/api/v1/auth/oauth/kakao/login") { param("id_token", "id-token-$u") }.andExpect { status { isOk() } }
            mockMvc.post("/api/v1/auth/oauth/kakao/login") { contentType = MediaType.APPLICATION_JSON }
                .andExpect { status { isBadRequest() } }
        }

        @Test
        fun `register·register valid - 본문 idToken`() {
            val oauth = OauthInfo(OauthProvider.KAKAO, "reg763-${UUID.randomUUID()}")
            doReturn(oauth).`when`(kakaoOauthHelper).getOauthInfoByIdToken("reg-$oauth")

            mockMvc.post("/api/v1/auth/oauth/kakao/register/valid") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("idToken" to "reg-$oauth"))
            }.andExpect { status { isOk() } }.data().let { assertTrue(it.at("/canRegister").asBoolean()) }

            mockMvc.post("/api/v1/auth/oauth/kakao/register") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("idToken" to "reg-$oauth", "email" to "reg763@test.com", "name" to "가입자", "marketingAgree" to false))
            }.andExpect { status { isOk() } }

            // 기존 GET(쿼리)도 그대로
            mockMvc.get("/api/v1/auth/oauth/kakao/register/valid") { param("id_token", "reg-$oauth") }
                .andExpect { status { isOk() } }.data().let { assertEquals(false, it.at("/canRegister").asBoolean()) }
        }

        @Test
        fun `kakao info - 본문 accessToken, 쿼리 access_token`() {
            val info = KakaoUserInfoDto("kakao-1", "k@test.com", null, null, "카카오", OauthProvider.KAKAO)
            doReturn(info).`when`(kakaoOauthHelper).getUserInfo("kakao-at")

            mockMvc.post("/api/v1/auth/oauth/kakao/info") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("accessToken" to "kakao-at"))
            }.andExpect { status { isOk() } }.data().let { assertEquals("카카오", it.at("/name").asText()) }
            mockMvc.post("/api/v1/auth/oauth/kakao/info") { param("access_token", "kakao-at") }.andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("7. 카카오 redirect_uri 허용 목록 + state")
    inner class OauthState {

        @Test
        fun `링크 - 허용 목록의 redirect_uri 와 state, 같은 값의 HttpOnly 쿠키`() {
            val res = mockMvc.get("/api/v1/auth/oauth/kakao/link") { header("Referer", "http://localhost:5173/") }
                .andExpect { status { isOk() } }
            val link = res.data().at("/link").asText()
            assertTrue(link.contains("redirect_uri=http://localhost:5173/admin/kakao/callback"), link)
            val state = Regex("[?&]state=([A-Za-z0-9_-]+)").find(link)!!.groupValues[1]
            val cookie = res.setCookies().single { it.startsWith("${OauthStateHelper.COOKIE_NAME}=") }
            assertTrue(cookie.startsWith("${OauthStateHelper.COOKIE_NAME}=$state;") && cookie.contains("HttpOnly"), cookie)

            // 허용 목록 밖 Referer 는 기본값
            val other = mockMvc.get("/api/v1/auth/oauth/kakao/link") { header("Referer", "https://evil.example/admin") }
                .andExpect { status { isOk() } }.data().at("/link").asText()
            assertTrue(other.contains("redirect_uri=http://localhost:3000/kakao/callback"), other)
        }

        @Test
        fun `코드 교환 - state 일치면 통과(쿠키 삭제), 불일치면 400, 없으면 전환 기간이라 통과`() {
            val kakao = KakaoTokenResponse().apply { idToken = "id"; accessToken = "at" }
            doReturn(kakao).`when`(kakaoOauthHelper).getOauthToken(anyString(), anyString())

            mockMvc.get("/api/v1/auth/oauth/kakao") {
                param("code", "c"); param("state", "s1"); cookie(Cookie(OauthStateHelper.COOKIE_NAME, "s1"))
            }.andExpect { status { isOk() } }.setCookies().let { assertTrue(it.any { c -> c.contains("Max-Age=0") }, "$it") }

            val mismatch = mockMvc.get("/api/v1/auth/oauth/kakao") {
                param("code", "c"); param("state", "s2"); cookie(Cookie(OauthStateHelper.COOKIE_NAME, "s1"))
            }.andExpect { status { isBadRequest() } }
            assertEquals("AUTH_400_1", mismatch.code())

            mockMvc.get("/api/v1/auth/oauth/kakao") { param("code", "c") }.andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("5. 쿠키 인증 상태 변경 요청의 Origin")
    inner class OriginCheck {

        private fun createHostByCookie(u: User, origin: String? = null, referer: String? = null, bearer: Boolean = false) =
            mockMvc.post("/api/v2/hosts") {
                cookie(Cookie("accessToken", jwtTokenProvider.generateAccessToken(u.id!!)))
                origin?.let { header("Origin", it) }
                referer?.let { header("Referer", it) }
                if (bearer) header("Authorization", "Bearer ${jwtTokenProvider.generateAccessToken(u.id!!)}")
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "오리진", "contacts" to listOf(mapOf("type" to "EMAIL", "value" to "o@gosrock.band"))))
            }

        @Test
        fun `잘못된 Origin 은 403 (CORS 단계), Origin·Referer 없음은 403 AUTH_403_3`() {
            val u = newUser()
            createHostByCookie(u, origin = "https://evil.example").andExpect { status { isForbidden() } }
            assertEquals("AUTH_403_3", createHostByCookie(u).andExpect { status { isForbidden() } }.code())
            assertEquals("AUTH_403_3", createHostByCookie(u, referer = "https://evil.example/x").andExpect { status { isForbidden() } }.code())
        }

        @Test
        fun `허용 Origin·Referer 이거나 Authorization 헤더면 통과`() {
            val u = newUser()
            createHostByCookie(u, origin = "http://localhost:3000").andExpect { status { isOk() } }
            createHostByCookie(u, referer = "https://staging.dudoong.com/admin/").andExpect { status { isOk() } }
            createHostByCookie(u, bearer = true).andExpect { status { isOk() } }
        }

        @Test
        fun `쿠키와 다른 사용자의 Bearer 헤더가 함께 오면 헤더 사용자로 인증한다 (면제 토큰 = 인증 토큰)`() {
            val cookieUser = newUser("쿠키")
            val headerUser = newUser("헤더")
            mockMvc.get("/api/v1/users/me") {
                cookie(Cookie("accessToken", jwtTokenProvider.generateAccessToken(cookieUser.id!!)))
                header("Authorization", "Bearer ${jwtTokenProvider.generateAccessToken(headerUser.id!!)}")
            }.andExpect { status { isOk() } }.data().let { assertEquals(headerUser.id, it.at("/userId").asLong(), "$it") }
            // 헤더가 Bearer 가 아니면 쿠키로 인증하고 출처 검사 대상
            createHostByCookie(cookieUser).andExpect { status { isForbidden() } }
        }

        @Test
        fun `쿠키로 refresh·logout 은 Origin 없이도 된다 (SSR)`() {
            val tokens = tokenGenerateHelper.execute(newUser())
            mockMvc.post("/api/v1/auth/token/refresh") {
                cookie(Cookie("accessToken", tokens.accessToken), Cookie("refreshToken", tokens.refreshToken))
            }.andExpect { status { isOk() } }
            mockMvc.post("/api/v1/auth/logout") { cookie(Cookie("accessToken", tokens.accessToken)) }.andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("8. 운영 어드민 감사 기록")
    inner class AdminAudit {

        private fun newAdmin(role: AccountRole = AccountRole.ADMIN): User =
            newUser("운영자").also { it.changeRole(role) }.let { userRepository.save(it) }

        private fun logsOf(actor: User): List<AdminAuditLog> =
            adminAuditLogRepository.findAll().filter { it.actorUserId == actor.id }.sortedBy { it.id }

        private fun setStatus(admin: User, target: User, status: String) =
            mockMvc.patch("/internal-api/v1/users/${target.id}/status") {
                with(user(admin.id.toString()).roles(admin.accountRole.value.removePrefix("ROLE_")))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("status" to status))
            }

        @Test
        fun `상태 변경 - 누가·무엇을·대상·요청·변경 전후`() {
            val admin = newAdmin()
            val target = newUser("대상")
            setStatus(admin, target, "SUSPENDED").andExpect { status { isOk() } }

            val log = logsOf(admin).single()
            assertEquals("AdminUserController.updateUserStatus", log.action)
            assertEquals("PATCH", log.httpMethod)
            assertEquals("/internal-api/v1/users/${target.id}/status", log.requestPath)
            assertEquals("{\"userId\":\"${target.id}\"}", log.target)
            assertEquals("{\"status\":\"SUSPENDED\"}", log.requestDetail)
            assertTrue(log.beforeValue!!.contains("\"account_state\":\"NORMAL\""), log.beforeValue)
            assertTrue(log.afterValue!!.contains("\"account_state\":\"SUSPENDED\""), log.afterValue)
            assertEquals(AdminAuditLog.RESULT_SUCCESS, log.result)
            assertNull(log.errorCode)
        }

        @Test
        fun `실패한 변경도 오류 코드와 함께 남는다`() {
            val admin = newAdmin()
            val superAdmin = newAdmin(AccountRole.SUPER_ADMIN)
            setStatus(admin, superAdmin, "SUSPENDED").andExpect { status { is4xxClientError() } }

            val log = logsOf(admin).single()
            assertEquals(AdminAuditLog.RESULT_FAIL, log.result)
            assertNotNull(log.errorCode)
            assertNull(log.afterValue)
        }

        @Test
        fun `감사 테이블이 없어도 어드민 요청은 성공하고 응답(호스트 상세·멤버)도 정상이다`() {
            val admin = newAdmin()
            val team = Team()
            jdbcTemplate.execute("ALTER TABLE tbl_admin_audit_log RENAME TO tbl_admin_audit_log_763_bak")
            try {
                val body = mockMvc.patch("/internal-api/v1/hosts/${team.hostId}/partner") {
                    with(user(admin.id.toString()).roles("ADMIN"))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("partner" to true))
                }.andExpect { status { isOk() } }.data()
                assertEquals(team.hostId, body.at("/id").asLong(), "$body")
                assertEquals(team.master.id, body.at("/masterUserId").asLong(), "$body")
                assertEquals(3, body.at("/memberCount").asInt(), "멤버(지연 로딩)까지 직렬화: $body")
                assertTrue(body.at("/partner").asBoolean(), "$body")
                mockMvc.get("/internal-api/v1/users/export") { with(user(admin.id.toString()).roles("ADMIN")) }
                    .andExpect { status { isOk() } }
            } finally {
                jdbcTemplate.execute("ALTER TABLE tbl_admin_audit_log_763_bak RENAME TO tbl_admin_audit_log")
            }
            assertEquals(emptyList<AdminAuditLog>(), logsOf(admin))
        }

        @Test
        fun `개인정보 최소화 - 허용 키만 값, 이름·검색어는 길이만, 사용자 스냅샷에 이름 없음`() {
            val admin = newAdmin()
            val target = newUser("대상자")
            mockMvc.patch("/internal-api/v1/users/${target.id}/name") {
                with(user(admin.id.toString()).roles("ADMIN"))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "새이름"))
            }.andExpect { status { isOk() } }
            setStatus(admin, target, "SUSPENDED").andExpect { status { isOk() } }

            val (rename, status) = logsOf(admin)
            assertEquals("{\"name\":\"***(len=3)\"}", rename.requestDetail)
            assertTrue(!rename.beforeValue!!.contains("대상자") && !rename.afterValue!!.contains("새이름"), "${rename.beforeValue} ${rename.afterValue}")
            assertEquals("{\"status\":\"SUSPENDED\"}", status.requestDetail)
        }

        @Test
        fun `엑셀 반출은 필터(요청 파라미터)와 함께 남고 일반 조회는 남지 않는다`() {
            val admin = newAdmin()
            mockMvc.get("/internal-api/v1/users/export") {
                with(user(admin.id.toString()).roles("ADMIN"))
                param("keyword", "없는사람")
            }.andExpect { status { isOk() } }
            mockMvc.get("/internal-api/v1/users") { with(user(admin.id.toString()).roles("ADMIN")) }.andExpect { status { isOk() } }

            val log = logsOf(admin).single()
            assertEquals("AdminUserController.exportUsers", log.action)
            assertEquals("{\"keyword\":\"***(len=4)\"}", log.requestDetail)
        }
    }

    @Nested
    @DisplayName("9. SUPER_ADMIN 예외 (v1 공연 생성·준비중 상세 = v2·AOP 와 같은 기준) + 감사 로그")
    inner class SuperAdminBypass {

        private val appender = ListAppender<ILoggingEvent>()
        private val auditLogger = LoggerFactory.getLogger("AUDIT.SuperAdminBypass") as Logger

        @BeforeEach
        fun attach() {
            appender.start()
            auditLogger.addAppender(appender)
        }

        @AfterEach
        fun detach() {
            auditLogger.detachAppender(appender)
            appender.stop()
        }

        private fun bypassArgs(userId: Long): List<String> =
            appender.list.filter { it.argumentArray?.getOrNull(0) == userId }.map { "${it.argumentArray[1]}|${it.argumentArray[2]}:${it.argumentArray[3]}" }

        private fun v1CreateEvent(u: User, hostId: Long) =
            mockMvc.post("/api/v1/events") {
                with(auth(u))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("hostId" to hostId, "name" to "v1 공연", "startAt" to eventStart.f(), "runTime" to 90))
            }

        @Test
        fun `v1 공연 생성 - SUPER_ADMIN 은 멤버가 아니어도 되고 감사 로그, 외부인은 거부`() {
            val team = Team()
            val admin = superAdmin()
            v1CreateEvent(admin, team.hostId).andExpect { status { isOk() } }
            v1CreateEvent(team.outsider, team.hostId).andExpect { status { is4xxClientError() } }
            assertEquals(listOf("CreateEventUseCase.execute|HOST:${team.hostId}"), bypassArgs(admin.id!!))
            assertEquals(emptyList<String>(), bypassArgs(team.outsider.id!!))
        }

        @Test
        fun `v1 준비중 공연 상세 - SUPER_ADMIN 은 볼 수 있고 감사 로그, 외부인은 거부, 멤버는 로그 없음`() {
            val team = Team()
            val admin = superAdmin()
            mockMvc.get("/api/v1/events/${team.eventId}") { with(auth(admin)) }.andExpect { status { isOk() } }
            mockMvc.get("/api/v1/events/${team.eventId}") { with(auth(team.outsider)) }.andExpect { status { is4xxClientError() } }
            mockMvc.get("/api/v1/events/${team.eventId}") { with(auth(team.guest)) }.andExpect { status { isOk() } }
            assertEquals(listOf("ReadEventDetailUseCase.execute|EVENT:${team.eventId}"), bypassArgs(admin.id!!))
            assertEquals(emptyList<String>(), bypassArgs(team.guest.id!!))
        }

        @Test
        fun `없는 호스트·공연이면 SUPER_ADMIN 이어도 404 (존재 확인은 예외 없음), 감사 로그도 없음`() {
            val admin = superAdmin()
            v1CreateEvent(admin, 99_999_999L).andExpect { status { isNotFound() } }
            mockMvc.get("/api/v2/events/99999999/manage") { with(auth(admin)) }.andExpect { status { isNotFound() } }
            mockMvc.get("/api/v2/hosts/99999999/members") { with(auth(admin)) }.andExpect { status { isNotFound() } }
            assertEquals(emptyList<String>(), bypassArgs(admin.id!!))
        }

        @Test
        fun `호스트 AOP(@HostRolesAllowed) 예외도 같은 감사 로그`() {
            val team = Team()
            val admin = superAdmin()
            mockMvc.get("/api/v2/events/${team.eventId}/manage") { with(auth(admin)) }.andExpect { status { isOk() } }
            val logs = bypassArgs(admin.id!!)
            assertEquals(1, logs.size, "$logs")
            assertTrue(logs.single().endsWith("|EVENT:${team.eventId}"), "$logs")
        }
    }
}
