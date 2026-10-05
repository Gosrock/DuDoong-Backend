package band.gosrock.api.v2.host

import band.gosrock.api.v2.support.V2ImageKeys
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.repository.HostFollowRepository
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.repository.UserRepository
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * v2 호스트/멤버 API 통합 테스트 (#704).
 * 컨텍스트(H2)가 테스트 간에 공유되므로 테스트마다 고유 이메일/호스트를 새로 만든다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 호스트 / 멤버 API")
class V2HostControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var hostRepository: HostRepository

    @Autowired private lateinit var hostFollowRepository: HostFollowRepository

    @Autowired private lateinit var eventRepository: EventRepository

    @Autowired private lateinit var presignedUrlService: S3UploadPresignedUrlService

    @Autowired @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    // ===== fixtures =====

    private fun newUser(name: String = "유저", email: String = "v2host-${UUID.randomUUID()}@test.com"): User {
        return userRepository.save(
            User(
                profile = Profile(name = name, email = email, phoneNumber = null, profileImage = null),
                oauthInfo = OauthInfo(OauthProvider.KAKAO, "v2host-${UUID.randomUUID()}"),
            ),
        )
    }

    private fun User.email(): String = profile!!.email!!

    private fun auth(u: User) = user(u.id.toString()).roles("USER")

    private fun json(body: Any): String = objectMapper.writeValueAsString(body)

    private fun ResultActionsDsl.body(): JsonNode =
        objectMapper.readTree(andReturn().response.getContentAsString(Charsets.UTF_8))

    private val defaultContacts = listOf(
        mapOf("type" to "INSTAGRAM", "value" to "@gosrock"),
        mapOf("type" to "EMAIL", "value" to "host@gosrock.band"),
        mapOf("type" to "PHONE", "value" to "010-1234-5678"),
        mapOf("type" to "EMAIL", "value" to "second@gosrock.band"),
    )

    /** v2 API 로 호스트 생성 → hostId */
    private fun createHost(master: User, name: String = "고스락", contacts: List<Map<String, String>> = defaultContacts): Long =
        mockMvc.post("/api/v2/hosts") {
            with(auth(master))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to name, "introduce" to "소개", "contacts" to contacts))
        }.andExpect { status { isOk() } }.body().at("/data/hostId").asLong()

    /** 마스터 + 매니저 + 일반 멤버가 있는 호스트 */
    private inner class Team {
        val master = newUser("마스터")
        val manager = newUser("매니저")
        val guest = newUser("일반")
        val outsider = newUser("외부인")
        val hostId = createHost(master)

        init {
            addMembers(master, hostId, listOf(manager.email() to "MANAGER", guest.email() to "GUEST"))
                .andExpect { status { isOk() } }
        }
    }

    private fun addMembers(requester: User, hostId: Long, members: List<Pair<String, String>>): ResultActionsDsl =
        mockMvc.post("/api/v2/hosts/$hostId/members") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("members" to members.map { mapOf("email" to it.first, "role" to it.second) }))
        }

    /** v1 초대 대기(비활성) 멤버를 직접 넣는다 */
    private fun addPendingMember(hostId: Long, user: User, role: HostRole) {
        val host = hostRepository.findById(hostId).get()
        host.hostUsers.add(HostUser(host = host, userId = user.id, role = role))
        hostRepository.save(host)
    }

    private fun saveEvent(hostId: Long, status: EventStatus, startAt: LocalDateTime?): Event {
        val event = Event(hostId = hostId, name = "공연-$status", startAt = startAt, runTime = 60L)
        ReflectionTestUtils.setField(event, "status", status)
        return eventRepository.save(event)
    }

    private fun memberRoles(body: JsonNode): Map<Long, String> =
        body.at("/data").associate { it["userId"].asLong() to it["role"].asText() }

    // ===== H-2 / H-3 =====

    @Nested
    @DisplayName("H-2 생성 / H-3 공개 홈")
    inner class CreateAndHome {

        @Test
        fun `생성하면 생성자가 활성 마스터가 되고 연락처가 순서대로 저장된다`() {
            val master = newUser()
            val hostId = createHost(master)

            val host = hostRepository.findById(hostId).get()
            assertEquals(master.id, host.masterUserId)
            val hostUser = host.getHostUserByUserId(master.id!!)
            assertEquals(HostRole.MASTER, hostUser.role)
            assertTrue(hostUser.active)

            mockMvc.get("/api/v2/hosts/$hostId") { with(auth(master)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("고스락") }
                jsonPath("$.data.introduce") { value("소개") }
                jsonPath("$.data.contacts.length()") { value(4) }
                jsonPath("$.data.contacts[0].type") { value("INSTAGRAM") }
                jsonPath("$.data.contacts[3].value") { value("second@gosrock.band") }
                jsonPath("$.data.memberCount") { value(1) }
                jsonPath("$.data.myRole") { value("MASTER") }
                jsonPath("$.data.createdAt") { exists() }
            }
        }

        @Test
        fun `비로그인도 공개 홈을 볼 수 있고 멤버 목록과 slackUrl 은 노출되지 않는다`() {
            val team = Team()

            val data = mockMvc.get("/api/v2/hosts/${team.hostId}").andExpect {
                status { isOk() }
                jsonPath("$.data.memberCount") { value(3) }
                jsonPath("$.data.followerCount") { value(0) }
                jsonPath("$.data.isFollowing") { value(false) }
                jsonPath("$.data.myRole") { value(null as Any?) }
            }.body().at("/data")

            listOf("hostUsers", "masterUser", "members", "slackUrl", "partner").forEach {
                assertFalse(data.has(it), "공개 홈에 $it 노출: $data")
            }
        }

        @Test
        fun `비멤버 로그인 유저의 myRole 은 null, 멤버는 자기 역할`() {
            val team = Team()
            mockMvc.get("/api/v2/hosts/${team.hostId}") { with(auth(team.outsider)) }
                .andExpect { jsonPath("$.data.myRole") { value(null as Any?) } }
            mockMvc.get("/api/v2/hosts/${team.hostId}") { with(auth(team.guest)) }
                .andExpect { jsonPath("$.data.myRole") { value("GUEST") } }
        }

        @Test
        fun `연락처가 비어 있는 기존 호스트는 v1 연락처로 대체 표시된다`() {
            val master = newUser()
            val legacy = hostRepository.save(
                band.gosrock.domain.domains.host.domain.Host(
                    masterUserId = master.id,
                    name = "레거시",
                    contactEmail = "legacy@gosrock.band",
                    contactNumber = "010-0000-0000",
                ),
            )

            mockMvc.get("/api/v2/hosts/${legacy.id}").andExpect {
                status { isOk() }
                jsonPath("$.data.contacts.length()") { value(2) }
                jsonPath("$.data.contacts[0].type") { value("PHONE") }
                jsonPath("$.data.contacts[0].value") { value("010-0000-0000") }
                jsonPath("$.data.contacts[1].type") { value("EMAIL") }
                jsonPath("$.data.contacts[1].value") { value("legacy@gosrock.band") }
            }
        }

        @Test
        fun `연락처 0개, 이름 16자, PHONE 16자는 400`() {
            val master = newUser()
            listOf(
                mapOf("name" to "호스트", "contacts" to emptyList<Any>()),
                mapOf("name" to "가".repeat(16), "contacts" to defaultContacts),
                mapOf("name" to "호스트", "contacts" to listOf(mapOf("type" to "PHONE", "value" to "0".repeat(16)))),
                mapOf("name" to "호스트", "contacts" to listOf(mapOf("type" to "FAX", "value" to "1"))),
            ).forEach { body ->
                mockMvc.post("/api/v2/hosts") {
                    with(auth(master))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(body)
                }.andExpect { status { isBadRequest() } }
            }
        }

        @Test
        fun `없는 호스트는 404`() {
            mockMvc.get("/api/v2/hosts/987654321").andExpect { status { isNotFound() } }
        }
    }

    // ===== v1 호환 =====

    @Nested
    @DisplayName("v1 호환")
    inner class V1Compat {

        @Test
        fun `v2 연락처의 첫 EMAIL, 첫 PHONE 이 v1 GET hosts 의 contactEmail, contactNumber 로 보인다`() {
            val master = newUser()
            val hostId = createHost(master)

            mockMvc.get("/api/v1/hosts/$hostId") { with(auth(master)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.contactEmail") { value("host@gosrock.band") }
                jsonPath("$.data.contactNumber") { value("010-1234-5678") }
            }
        }

        @Test
        fun `v2 수정으로 PHONE 이 빠져도 v1 contactNumber 는 기존 값을 유지한다`() {
            val master = newUser()
            val hostId = createHost(master)

            mockMvc.patch("/api/v2/hosts/$hostId") {
                with(auth(master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("contacts" to listOf(mapOf("type" to "EMAIL", "value" to "new@gosrock.band"))))
            }.andExpect { status { isOk() } }

            mockMvc.get("/api/v1/hosts/$hostId") { with(auth(master)) }.andExpect {
                jsonPath("$.data.contactEmail") { value("new@gosrock.band") }
                jsonPath("$.data.contactNumber") { value("010-1234-5678") }
            }
        }

        @Test
        fun `v2 연락처가 있는 호스트를 v1 PATCH profile 로 수정하면 v2 홈의 첫 EMAIL, PHONE 이 바뀐다`() {
            val master = newUser()
            val hostId = createHost(master)

            mockMvc.patch("/api/v1/hosts/$hostId/profile") {
                with(auth(master))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf(
                        "profileImageKey" to "v1/profile.png",
                        "introduce" to "v1 소개",
                        "contactNumber" to "010-9999-8888",
                        "contactEmail" to "v1@gosrock.band",
                    ),
                )
            }.andExpect {
                status { isOk() }
                // v1 응답 형식 그대로
                jsonPath("$.data.contactEmail") { value("v1@gosrock.band") }
                jsonPath("$.data.contacts") { doesNotExist() }
            }

            mockMvc.get("/api/v2/hosts/$hostId").andExpect {
                jsonPath("$.data.contacts.length()") { value(4) }
                jsonPath("$.data.contacts[0].value") { value("@gosrock") }
                jsonPath("$.data.contacts[1].value") { value("v1@gosrock.band") }
                jsonPath("$.data.contacts[2].value") { value("010-9999-8888") }
                jsonPath("$.data.contacts[3].value") { value("second@gosrock.band") }
            }
        }

        @Test
        fun `v2 로 추가한 멤버는 v1 목록에 활성으로 보이고, v2 로 삭제하면 v1 목록에서 빠진다`() {
            val team = Team()

            mockMvc.get("/api/v1/hosts") { with(auth(team.guest)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.content[?(@.hostId == ${team.hostId})].active") { value(true) }
            }

            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.guest.id}") { with(auth(team.master)) }
                .andExpect { status { isOk() } }

            val hostIds = mockMvc.get("/api/v1/hosts") { with(auth(team.guest)) }
                .andExpect { status { isOk() } }
                .body().at("/data/content").map { it["hostId"].asLong() }
            assertFalse(hostIds.contains(team.hostId), "v1 목록에 삭제된 호스트가 남아 있음: $hostIds")
        }
    }

    // ===== H-1 =====

    @Nested
    @DisplayName("H-1 내 호스트 목록")
    inner class MyHosts {

        @Test
        fun `활성 호스트만, 역할과 삭제 제외 공연 수, 키워드 부분일치, 페이지 정보`() {
            val me = newUser()
            val other = newUser()
            val hostA = createHost(me, name = "알파밴드")
            val hostB = createHost(other, name = "베타밴드")
            addMembers(other, hostB, listOf(me.email() to "GUEST")).andExpect { status { isOk() } }
            // v1 초대 대기(비활성) 호스트는 제외
            val pending = hostRepository.save(band.gosrock.domain.domains.host.domain.Host(masterUserId = other.id, name = "대기밴드"))
            pending.hostUsers.add(HostUser(host = pending, userId = me.id, role = HostRole.GUEST))
            hostRepository.save(pending)

            saveEvent(hostA, EventStatus.PREPARING, null)
            saveEvent(hostA, EventStatus.OPEN, LocalDateTime.now().plusDays(3))
            saveEvent(hostA, EventStatus.DELETED, LocalDateTime.now().plusDays(3))

            val body = mockMvc.get("/api/v2/me/hosts") { with(auth(me)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.totalElements") { value(2) }
                jsonPath("$.data.page") { value(0) }
                jsonPath("$.data.size") { value(10) }
                jsonPath("$.data.hasNext") { value(false) }
            }.body().at("/data/content")
            val byId = body.associateBy { it["hostId"].asLong() }
            assertEquals(setOf(hostA, hostB), byId.keys)
            assertEquals("MASTER", byId.getValue(hostA)["myRole"].asText())
            assertEquals(2, byId.getValue(hostA)["eventCount"].asLong())
            assertEquals("GUEST", byId.getValue(hostB)["myRole"].asText())
            assertEquals(0, byId.getValue(hostB)["eventCount"].asLong())

            mockMvc.get("/api/v2/me/hosts?keyword=베타") { with(auth(me)) }.andExpect {
                jsonPath("$.data.totalElements") { value(1) }
                jsonPath("$.data.content[0].hostId") { value(hostB) }
            }
            mockMvc.get("/api/v2/me/hosts?page=0&size=1") { with(auth(me)) }.andExpect {
                jsonPath("$.data.content.length()") { value(1) }
                jsonPath("$.data.totalPages") { value(2) }
                jsonPath("$.data.hasNext") { value(true) }
            }
        }

        @Test
        fun `비로그인은 401, size 범위 밖은 400`() {
            mockMvc.get("/api/v2/me/hosts").andExpect { status { isUnauthorized() } }
            mockMvc.get("/api/v2/me/hosts?size=0") { with(auth(newUser())) }.andExpect { status { isBadRequest() } }
        }
    }

    // ===== H-4 =====

    @Nested
    @DisplayName("H-4 호스트 수정")
    inner class Update {

        @Test
        fun `매니저가 부분 수정하면 null 필드는 유지되고 빈 문자열은 비운다`() {
            val team = Team()
            val prefix = presignedUrlService.hostImageKeyPrefix(team.hostId)
            val profileKey = V2ImageKeys.issued(prefix)
            val coverKey = V2ImageKeys.issued(prefix, "jpeg")

            mockMvc.patch("/api/v2/hosts/${team.hostId}") {
                with(auth(team.manager))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "새이름", "profileImageKey" to profileKey, "coverImageKey" to coverKey))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("새이름") }
                jsonPath("$.data.introduce") { value("소개") }
                jsonPath("$.data.contacts.length()") { value(4) }
                jsonPath("$.data.profileImageUrl") { value(org.hamcrest.Matchers.endsWith(profileKey)) }
                jsonPath("$.data.coverImageUrl") { value(org.hamcrest.Matchers.endsWith(coverKey)) }
            }

            mockMvc.patch("/api/v2/hosts/${team.hostId}") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("introduce" to "", "coverImageKey" to ""))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("새이름") }
                jsonPath("$.data.introduce") { value(null as Any?) }
                jsonPath("$.data.coverImageUrl") { value(null as Any?) }
                jsonPath("$.data.profileImageUrl") { value(org.hamcrest.Matchers.endsWith(profileKey)) }
            }

            // 실제 저장 확인
            mockMvc.get("/api/v2/hosts/${team.hostId}").andExpect {
                jsonPath("$.data.name") { value("새이름") }
                jsonPath("$.data.coverImageUrl") { value(null as Any?) }
            }
        }

        @Test
        fun `이미지 key 는 이 호스트에 발급한 형식(prefix + UUID + jpeg·jpg·png)만 허용 — 외부·카카오 URL, 다른 호스트 key, 경로 조작(인코딩 포함), 다른 확장자·쿼리는 400`() {
            val team = Team()
            val otherHostId = createHost(newUser())
            V2ImageKeys.rejected(presignedUrlService.hostImageKeyPrefix(team.hostId), presignedUrlService.hostImageKeyPrefix(otherHostId)).forEach { key ->
                listOf("profileImageKey", "coverImageKey").forEach { field ->
                    mockMvc.patch("/api/v2/hosts/${team.hostId}") {
                        with(auth(team.master))
                        contentType = MediaType.APPLICATION_JSON
                        content = json(mapOf(field to key))
                    }.andExpect {
                        status { isBadRequest() }
                        jsonPath("$.code") { value("HOST_400_17") }
                    }
                }
            }
        }

        @Test
        fun `일반 멤버는 403, 비멤버는 403, 빈 연락처 교체는 400`() {
            val team = Team()
            listOf(team.guest, team.outsider).forEach { requester ->
                mockMvc.patch("/api/v2/hosts/${team.hostId}") {
                    with(auth(requester))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("name" to "x"))
                }.andExpect { status { isForbidden() } }
            }
            mockMvc.patch("/api/v2/hosts/${team.hostId}") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("contacts" to emptyList<Any>()))
            }.andExpect { status { isBadRequest() } }
        }
    }

    // ===== H-6 / H-7 =====

    @Nested
    @DisplayName("H-6 멤버 목록 / H-7 멤버 추가")
    inner class Members {

        @Test
        fun `일반 멤버도 멤버 목록을 볼 수 있고 마스터-매니저-일반 순이다`() {
            val team = Team()
            mockMvc.get("/api/v2/hosts/${team.hostId}/members") { with(auth(team.guest)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(3) }
                jsonPath("$.data[0].userId") { value(team.master.id) }
                jsonPath("$.data[0].role") { value("MASTER") }
                jsonPath("$.data[0].name") { value("마스터") }
                jsonPath("$.data[1].role") { value("MANAGER") }
                jsonPath("$.data[2].role") { value("GUEST") }
            }
        }

        @Test
        fun `비멤버는 멤버 목록 403, 비로그인은 401`() {
            val team = Team()
            mockMvc.get("/api/v2/hosts/${team.hostId}/members") { with(auth(team.outsider)) }
                .andExpect { status { isForbidden() } }
            mockMvc.get("/api/v2/hosts/${team.hostId}/members").andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `추가된 멤버는 수락 없이 즉시 활성이다`() {
            val team = Team()
            val host = hostRepository.findById(team.hostId).get()
            assertTrue(host.isActiveHostUserId(team.manager.id!!))
            assertEquals(HostRole.MANAGER, host.getHostUserByUserId(team.manager.id!!).role)
            assertTrue(host.isActiveHostUserId(team.guest.id!!))
        }

        @Test
        fun `매니저는 GUEST 추가 가능, MANAGER 가 하나라도 있으면 403 이고 아무도 추가되지 않는다`() {
            val team = Team()
            val a = newUser()
            val b = newUser()

            addMembers(team.manager, team.hostId, listOf(a.email() to "GUEST", b.email() to "MANAGER")).andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("HOST_400_11") }
            }
            val host = hostRepository.findById(team.hostId).get()
            assertFalse(host.hasHostUserId(a.id!!))

            addMembers(team.manager, team.hostId, listOf(a.email() to "GUEST")).andExpect { status { isOk() } }
        }

        @Test
        fun `일반 멤버는 추가 403`() {
            val team = Team()
            addMembers(team.guest, team.hostId, listOf(newUser().email() to "GUEST"))
                .andExpect { status { isForbidden() } }
        }

        @Test
        fun `MASTER 지정은 400`() {
            val team = Team()
            addMembers(team.master, team.hostId, listOf(newUser().email() to "MASTER")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_10") }
            }
        }

        @Test
        fun `미가입 이메일이 있으면 전체 롤백되고 reason 에 해당 이메일이 담긴다`() {
            val team = Team()
            val ok = newUser()
            val unknown = "nobody-${UUID.randomUUID()}@test.com"

            addMembers(team.master, team.hostId, listOf(ok.email() to "GUEST", unknown to "GUEST")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_13") }
                jsonPath("$.reason") { value(org.hamcrest.Matchers.containsString(unknown)) }
            }
            assertFalse(hostRepository.findById(team.hostId).get().hasHostUserId(ok.id!!))
        }

        @Test
        fun `같은 이메일의 정상 계정이 여러 개면 400 이고 해당 이메일이 담긴다`() {
            val team = Team()
            val email = "dup-${UUID.randomUUID()}@test.com"
            newUser(email = email)
            newUser(email = email)

            addMembers(team.master, team.hostId, listOf(email to "GUEST")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_18") }
                jsonPath("$.reason") { value(org.hamcrest.Matchers.containsString(email)) }
            }
        }

        @Test
        fun `같은 멤버를 동시에 추가하면 하나만 성공하고 나머지는 이미 멤버 400 이다 (500 아님)`() {
            val team = Team()
            val target = newUser()
            val pool = Executors.newFixedThreadPool(4)
            try {
                val statuses = (1..4).map {
                    pool.submit(
                        Callable {
                            addMembers(team.master, team.hostId, listOf(target.email() to "GUEST"))
                                .andReturn().response.status
                        },
                    )
                }.map { it.get() }

                assertEquals(1, statuses.count { it == 200 }, "statuses=$statuses")
                assertEquals(3, statuses.count { it == 400 }, "statuses=$statuses")
            } finally {
                pool.shutdown()
            }
            assertEquals(1, hostRepository.findById(team.hostId).get().hostUsers.count { it.userId == target.id })
        }

        @Test
        fun `이미 멤버인 이메일, 요청 내 중복 이메일은 400 이고 해당 이메일이 담긴다`() {
            val team = Team()
            addMembers(team.master, team.hostId, listOf(team.guest.email() to "GUEST")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_15") }
                jsonPath("$.reason") { value(org.hamcrest.Matchers.containsString(team.guest.email())) }
            }
            val dup = newUser()
            addMembers(team.master, team.hostId, listOf(dup.email() to "GUEST", dup.email().uppercase() to "MANAGER")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_14") }
            }
        }
    }

    // ===== H-10 / H-11 / H-12 =====

    @Nested
    @DisplayName("H-10 역할 변경 / H-11 삭제 / H-12 마스터 양도")
    inner class RoleAndRemoveAndTransfer {

        @Test
        fun `마스터는 GUEST 와 MANAGER 를 서로 바꿀 수 있다`() {
            val team = Team()
            val body = mockMvc.patch("/api/v2/hosts/${team.hostId}/members/${team.guest.id}/role") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("role" to "MANAGER"))
            }.andExpect { status { isOk() } }.body()
            assertEquals("MANAGER", memberRoles(body)[team.guest.id])

            mockMvc.patch("/api/v2/hosts/${team.hostId}/members/${team.manager.id}/role") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("role" to "GUEST"))
            }.andExpect { status { isOk() } }
            assertEquals(HostRole.GUEST, hostRepository.findById(team.hostId).get().getHostUserByUserId(team.manager.id!!).role)
        }

        @Test
        fun `매니저는 역할 변경 403, 마스터 대상 400, MASTER 지정 400`() {
            val team = Team()
            fun patchRole(requester: User, target: User, role: String) =
                mockMvc.patch("/api/v2/hosts/${team.hostId}/members/${target.id}/role") {
                    with(auth(requester))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("role" to role))
                }
            patchRole(team.manager, team.guest, "MANAGER").andExpect { status { isForbidden() } }
            patchRole(team.master, team.master, "GUEST").andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_5") }
            }
            patchRole(team.master, team.guest, "MASTER").andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_10") }
            }
        }

        @Test
        fun `매니저는 일반 멤버를 삭제할 수 있고 매니저-마스터는 삭제할 수 없다`() {
            val team = Team()
            val otherManager = newUser()
            addMembers(team.master, team.hostId, listOf(otherManager.email() to "MANAGER")).andExpect { status { isOk() } }

            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${otherManager.id}") { with(auth(team.manager)) }
                .andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("HOST_400_11") }
                }
            // 본인(매니저) 삭제도 같은 규칙으로 막힌다
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.manager.id}") { with(auth(team.manager)) }
                .andExpect { status { isForbidden() } }
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.master.id}") { with(auth(team.manager)) }
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("HOST_400_12") }
                }

            val body = mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.guest.id}") { with(auth(team.manager)) }
                .andExpect { status { isOk() } }.body()
            assertFalse(memberRoles(body).containsKey(team.guest.id))
            assertFalse(hostRepository.findById(team.hostId).get().hasHostUserId(team.guest.id!!))
        }

        @Test
        fun `마스터는 매니저를 삭제할 수 있고, 일반 멤버는 삭제 403, 없는 멤버는 404`() {
            val team = Team()
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.manager.id}") { with(auth(team.guest)) }
                .andExpect { status { isForbidden() } }
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.outsider.id}") { with(auth(team.master)) }
                .andExpect { status { isNotFound() } }
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.manager.id}") { with(auth(team.master)) }
                .andExpect { status { isOk() } }
        }

        @Test
        fun `마스터 양도 후 기존 마스터는 매니저, 비멤버-대기 멤버 대상은 404, 매니저 요청은 403`() {
            val team = Team()
            fun transfer(requester: User, target: User) =
                mockMvc.post("/api/v2/hosts/${team.hostId}/master-transfer") {
                    with(auth(requester))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("userId" to target.id))
                }

            transfer(team.manager, team.guest).andExpect { status { isForbidden() } }
            transfer(team.master, team.outsider).andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("HOST_404_2") }
            }
            val pending = newUser()
            addPendingMember(team.hostId, pending, HostRole.GUEST)
            transfer(team.master, pending).andExpect { status { isNotFound() } }

            val body = transfer(team.master, team.guest).andExpect { status { isOk() } }.body()
            val roles = memberRoles(body)
            assertEquals("MASTER", roles[team.guest.id])
            assertEquals("MANAGER", roles[team.master.id])
            assertEquals(team.guest.id, hostRepository.findById(team.hostId).get().masterUserId)
        }
    }

    // ===== 권한 경계 =====

    @Nested
    @DisplayName("권한 경계 (대기 멤버 / SUPER_ADMIN / IDOR)")
    inner class PermissionEdges {

        @Test
        fun `초대 대기 멤버는 G+, M+ API 모두 403`() {
            val team = Team()
            val pendingManager = newUser()
            addPendingMember(team.hostId, pendingManager, HostRole.MANAGER)

            mockMvc.get("/api/v2/hosts/${team.hostId}/members") { with(auth(pendingManager)) }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("HOST_400_6") }
            }
            mockMvc.patch("/api/v2/hosts/${team.hostId}") {
                with(auth(pendingManager))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "x"))
            }.andExpect { status { isForbidden() } }
            addMembers(pendingManager, team.hostId, listOf(newUser().email() to "GUEST"))
                .andExpect { status { isForbidden() } }
            // 공개 홈에서도 대기 멤버는 역할 없음
            mockMvc.get("/api/v2/hosts/${team.hostId}") { with(auth(pendingManager)) }
                .andExpect { jsonPath("$.data.myRole") { value(null as Any?) } }
        }

        @Test
        fun `매니저는 MANAGER 역할의 초대 대기 멤버를 삭제할 수 없고 마스터는 할 수 있다`() {
            val team = Team()
            val pendingManager = newUser()
            addPendingMember(team.hostId, pendingManager, HostRole.MANAGER)

            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${pendingManager.id}") { with(auth(team.manager)) }
                .andExpect {
                    status { isForbidden() }
                    jsonPath("$.code") { value("HOST_400_11") }
                }
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${pendingManager.id}") { with(auth(team.master)) }
                .andExpect { status { isOk() } }
            assertFalse(hostRepository.findById(team.hostId).get().hasHostUserId(pendingManager.id!!))
        }

        @Test
        fun `다른 호스트 hostId 로 요청하면 403, 내 호스트에서 다른 호스트 소속 userId 를 대상으로 하면 404`() {
            val mine = Team()
            val other = Team()

            mockMvc.get("/api/v2/hosts/${other.hostId}/members") { with(auth(mine.master)) }
                .andExpect { status { isForbidden() } }
            mockMvc.delete("/api/v2/hosts/${other.hostId}/members/${other.guest.id}") { with(auth(mine.master)) }
                .andExpect { status { isForbidden() } }
            mockMvc.patch("/api/v2/hosts/${other.hostId}/members/${other.guest.id}/role") {
                with(auth(mine.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("role" to "MANAGER"))
            }.andExpect { status { isForbidden() } }

            mockMvc.delete("/api/v2/hosts/${mine.hostId}/members/${other.guest.id}") { with(auth(mine.master)) }
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("HOST_404_2") }
                }
            mockMvc.patch("/api/v2/hosts/${mine.hostId}/members/${other.guest.id}/role") {
                with(auth(mine.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("role" to "MANAGER"))
            }.andExpect { status { isNotFound() } }
            // 다른 호스트 멤버는 그대로
            assertTrue(hostRepository.findById(other.hostId).get().isActiveHostUserId(other.guest.id!!))
        }

        /** 현재 동작 고정: AOP 권한 검사만 바이패스하고, 요청자 역할을 보는 도메인 규칙은 그대로 적용된다 */
        @Test
        fun `SUPER_ADMIN 비멤버는 조회-GUEST 추가-삭제-역할 변경은 되고 매니저 추가-삭제, 마스터 양도는 막힌다`() {
            val team = Team()
            val admin = newUser().also { it.changeRole(AccountRole.SUPER_ADMIN) }.let { userRepository.save(it) }
            val newGuest = newUser()
            val newManager = newUser()

            mockMvc.get("/api/v2/hosts/${team.hostId}/members") { with(auth(admin)) }.andExpect { status { isOk() } }
            addMembers(admin, team.hostId, listOf(newGuest.email() to "GUEST")).andExpect { status { isOk() } }
            addMembers(admin, team.hostId, listOf(newManager.email() to "MANAGER")).andExpect { status { isForbidden() } }
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${team.manager.id}") { with(auth(admin)) }
                .andExpect { status { isForbidden() } }
            mockMvc.patch("/api/v2/hosts/${team.hostId}/members/${team.guest.id}/role") {
                with(auth(admin))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("role" to "MANAGER"))
            }.andExpect { status { isOk() } }
            mockMvc.post("/api/v2/hosts/${team.hostId}/master-transfer") {
                with(auth(admin))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("userId" to team.manager.id))
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("HOST_400_2") }
            }
            mockMvc.delete("/api/v2/hosts/${team.hostId}/members/${newGuest.id}") { with(auth(admin)) }
                .andExpect { status { isOk() } }
            // 공개 홈에서 SUPER_ADMIN 은 멤버가 아니므로 역할 없음
            mockMvc.get("/api/v2/hosts/${team.hostId}") { with(auth(admin)) }
                .andExpect { jsonPath("$.data.myRole") { value(null as Any?) } }
        }

        @Test
        fun `공개 경로는 숫자 hostId 만 허용한다`() {
            mockMvc.get("/api/v2/hosts/abc").andExpect { status { isUnauthorized() } }
            mockMvc.get("/api/v2/hosts/abc/events").andExpect { status { isUnauthorized() } }
            mockMvc.get("/api/v2/hosts/1/members").andExpect { status { isUnauthorized() } }
        }
    }

    // ===== DEC-013 호스트 삭제 미제공 (#721) =====

    @Nested
    @DisplayName("DEC-013 호스트 삭제 미제공")
    inner class NoHostDelete {

        @Test
        fun `호스트 경로에 DELETE 매핑이 없다`() {
            val deletePatterns = handlerMapping.handlerMethods.keys
                .filter { RequestMethod.DELETE in it.methodsCondition.methods }
                .flatMap { it.pathPatternsCondition?.patternValues ?: it.patternsCondition?.patterns.orEmpty() }
            assertTrue(deletePatterns.none { it.matches(Regex("/api/v2/hosts/\\{[^}]+}/?")) }, "호스트 삭제 매핑이 생김: $deletePatterns")
        }

        @Test
        fun `마스터가 DELETE 호출하면 405 (같은 경로에 GET·PATCH 만 있음) 이고 호스트는 남아 있다`() {
            val team = Team()
            mockMvc.delete("/api/v2/hosts/${team.hostId}") { with(auth(team.master)) }
                .andExpect { status { isMethodNotAllowed() } }
            assertTrue(hostRepository.findById(team.hostId).isPresent)
        }
    }

    // ===== H-13 =====

    @Nested
    @DisplayName("H-13 팔로우")
    inner class Follow {

        @Test
        fun `팔로우와 해제는 멱등이고 공개 홈에 반영된다`() {
            val team = Team()
            val fan = newUser()
            repeat(2) {
                mockMvc.put("/api/v2/hosts/${team.hostId}/follow") { with(auth(fan)) }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.isFollowing") { value(true) }
                    jsonPath("$.data.followerCount") { value(1) }
                }
            }
            assertEquals(1, hostFollowRepository.countByHostId(team.hostId))
            mockMvc.get("/api/v2/hosts/${team.hostId}") { with(auth(fan)) }.andExpect {
                jsonPath("$.data.isFollowing") { value(true) }
                jsonPath("$.data.followerCount") { value(1) }
            }
            mockMvc.get("/api/v2/hosts/${team.hostId}").andExpect {
                jsonPath("$.data.isFollowing") { value(false) }
                jsonPath("$.data.followerCount") { value(1) }
            }

            repeat(2) {
                mockMvc.delete("/api/v2/hosts/${team.hostId}/follow") { with(auth(fan)) }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.isFollowing") { value(false) }
                    jsonPath("$.data.followerCount") { value(0) }
                }
            }
        }

        @Test
        fun `비로그인 팔로우는 401, 없는 호스트는 404`() {
            mockMvc.put("/api/v2/hosts/1/follow").andExpect { status { isUnauthorized() } }
            mockMvc.put("/api/v2/hosts/987654321/follow") { with(auth(newUser())) }.andExpect { status { isNotFound() } }
        }
    }

    // ===== H-14 =====

    @Nested
    @DisplayName("H-14 호스트 공연 리스트")
    inner class HostEvents {

        @Test
        fun `비멤버-비로그인은 공개 공연만, 멤버는 준비중 포함, 삭제는 항상 제외`() {
            val team = Team()
            val now = LocalDateTime.now()
            val preparing = saveEvent(team.hostId, EventStatus.PREPARING, null)
            val upcoming = saveEvent(team.hostId, EventStatus.OPEN, now.plusDays(7))
            val startedOpen = saveEvent(team.hostId, EventStatus.OPEN, now.minusDays(1))
            val closed = saveEvent(team.hostId, EventStatus.CLOSED, now.minusDays(30))
            val calculating = saveEvent(team.hostId, EventStatus.CALCULATING, now.minusDays(2))
            // 시작 ~ 종료(시작 + 60분) 사이 (#716 표시 상태 ONGOING)
            val ongoing = saveEvent(team.hostId, EventStatus.OPEN, now.minusMinutes(30))
            saveEvent(team.hostId, EventStatus.DELETED, now.plusDays(1))

            val publicIds = mockMvc.get("/api/v2/hosts/${team.hostId}/events")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.data.totalElements") { value(5) }
                }.body().at("/data/content").map { it["eventId"].asLong() }
            assertEquals(listOf(ongoing.id, calculating.id, closed.id, startedOpen.id, upcoming.id), publicIds)

            mockMvc.get("/api/v2/hosts/${team.hostId}/events") { with(auth(team.outsider)) }
                .andExpect { jsonPath("$.data.totalElements") { value(5) } }

            val memberContent = mockMvc.get("/api/v2/hosts/${team.hostId}/events") { with(auth(team.guest)) }
                .andExpect { jsonPath("$.data.totalElements") { value(6) } }
                .body().at("/data/content")
            val display = memberContent.associate { it["eventId"].asLong() to it["displayStatus"].asText() }
            assertEquals("PREPARING", display[preparing.id])
            assertEquals("UPCOMING", display[upcoming.id])
            assertEquals("ONGOING", display[ongoing.id])
            // 종료(시작 + 60분)가 지났지만 종료 배치 전인 OPEN
            assertEquals("PAST", display[startedOpen.id])
            assertEquals("PAST", display[closed.id])
            assertEquals("PAST", display[calculating.id])
            assertEquals("OPEN", memberContent.first { it["eventId"].asLong() == upcoming.id }["status"].asText())
        }

        @Test
        fun `카드 장소는 공연 상세와 같은 place 형태, 미입력이면 null, 주소만 있어도 표시 (#726)`() {
            val team = Team()
            val now = LocalDateTime.now()
            val withPlace = saveEvent(team.hostId, EventStatus.OPEN, now.plusDays(7)).also {
                ReflectionTestUtils.setField(it, "eventPlace", EventPlace(latitude = 37.548369, longitude = 126.920036, placeName = "롤링홀", placeAddress = "서울 마포구 어울마당로 35"))
                eventRepository.save(it)
            }
            val addressOnly = saveEvent(team.hostId, EventStatus.OPEN, now.plusDays(8)).also {
                ReflectionTestUtils.setField(it, "eventPlace", EventPlace(placeAddress = "서울 마포구"))
                eventRepository.save(it)
            }
            val noPlace = saveEvent(team.hostId, EventStatus.OPEN, now.plusDays(9))

            val byId = mockMvc.get("/api/v2/hosts/${team.hostId}/events").andExpect { status { isOk() } }
                .body().at("/data/content").associateBy { it["eventId"].asLong() }
            val place = byId.getValue(withPlace.id!!)["place"]
            assertEquals("롤링홀", place["name"].asText())
            assertEquals("서울 마포구 어울마당로 35", place["address"].asText())
            assertEquals(37.548369, place["latitude"].asDouble())
            assertEquals(126.920036, place["longitude"].asDouble())
            assertTrue(byId.getValue(addressOnly.id!!)["place"]["name"].isNull)
            assertEquals("서울 마포구", byId.getValue(addressOnly.id!!)["place"]["address"].asText())
            assertTrue(byId.getValue(noPlace.id!!)["place"].isNull)

            // P-3 공개 상세와 같은 값
            val detail = mockMvc.get("/api/v2/events/${withPlace.id}").andExpect { status { isOk() } }.body().at("/data/place")
            assertEquals(detail, place)
        }
    }

    // ===== H-15 =====

    @Nested
    @DisplayName("H-15 이미지 업로드 url")
    inner class Images {

        @Test
        fun `매니저는 COVER 업로드 url 을 받고, 일반 멤버는 403`() {
            val team = Team()
            mockMvc.post("/api/v2/hosts/${team.hostId}/images") {
                with(auth(team.manager))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("purpose" to "COVER", "extension" to "PNG"))
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.purpose") { value("COVER") }
                jsonPath("$.data.key") { value(org.hamcrest.Matchers.containsString("/host/${team.hostId}/")) }
                jsonPath("$.data.presignedUrl") { exists() }
                jsonPath("$.data.url") { exists() }
            }
            mockMvc.post("/api/v2/hosts/${team.hostId}/images") {
                with(auth(team.guest))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("purpose" to "PROFILE", "extension" to "PNG"))
            }.andExpect { status { isForbidden() } }
        }
    }
}
