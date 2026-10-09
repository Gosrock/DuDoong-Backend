package band.gosrock.api.v2.ticket

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.EventStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/** v2 옵션 API 통합 테스트 (#707): O-1 ~ O-5 + 판매된 티켓 잠금(DEC-012) + v1 호환 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 티켓 옵션 API")
class V2TicketOptionControllerTest : V2TicketApiTestSupport() {

    @Nested
    @DisplayName("O-2 생성 / O-1 목록")
    inner class Create {

        @Test
        fun `네·아니오와 주관식 생성, 목록에 적용 티켓·잠김 표시`() {
            val team = Team()
            postOption(team.manager, team.eventId, mapOf("name" to "뒷풀이", "description" to "참석?", "type" to "YES_NO", "yesAdditionalPrice" to 10000)).andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("뒷풀이") }
                jsonPath("$.data.description") { value("참석?") }
                jsonPath("$.data.type") { value("YES_NO") }
                jsonPath("$.data.yesAdditionalPrice") { value(10000) }
                jsonPath("$.data.appliedTicketItemIds.length()") { value(0) }
                jsonPath("$.data.isLocked") { value(false) }
            }
            val subjective = createOption(team.manager, team.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "입금자명")
            val ticket = createTicket(team.manager, team.eventId)
            putOptions(team.manager, team.eventId, ticket, listOf(subjective)).andExpect { status { isOk() } }

            options(team.guest, team.eventId).andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(2) }
                jsonPath("$.data[1].optionId") { value(subjective) }
                jsonPath("$.data[1].type") { value("SUBJECTIVE") }
                jsonPath("$.data[1].yesAdditionalPrice") { value(null as Any?) }
                jsonPath("$.data[1].appliedTicketItemIds[0]") { value(ticket) }
                jsonPath("$.data[1].isLocked") { value(false) }
            }
        }

        @Test
        fun `주관식 추가금, 음수, 객관식, 이름 21자, 설명 누락은 400`() {
            val team = Team()
            fun body(vararg pairs: Pair<String, Any?>) = mapOf("name" to "옵션", "description" to "설명", "type" to "YES_NO", "yesAdditionalPrice" to 0) + pairs
            postOption(team.manager, team.eventId, body("type" to "SUBJECTIVE", "yesAdditionalPrice" to 1000))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Option_Group_400_3") } }
            postOption(team.manager, team.eventId, body("type" to "MULTIPLE_CHOICE"))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Option_Group_400_4") } }
            postOption(team.manager, team.eventId, body("yesAdditionalPrice" to -1)).andExpect { status { isBadRequest() } }
            postOption(team.manager, team.eventId, body("name" to "가".repeat(21))).andExpect { status { isBadRequest() } }
            postOption(team.manager, team.eventId, body("description" to null)).andExpect { status { isBadRequest() } }
            postOption(team.manager, team.eventId, body("type" to "TRUE_FALSE")).andExpect { status { isBadRequest() } }
            postOption(team.manager, team.eventId, body("name" to "가".repeat(20))).andExpect { status { isOk() } }
        }

        @Test
        fun `권한 - 일반 생성 403, 일반 조회 200, 비멤버 조회 403, 비로그인 401, SUPER_ADMIN 생성, 다른 호스트 IDOR 403`() {
            val team = Team()
            val other = Team("다른호스트")
            val body = mapOf("name" to "옵션", "description" to "설명", "type" to "YES_NO")
            postOption(team.guest, team.eventId, body).andExpect { status { isForbidden() } }
            options(team.guest, team.eventId).andExpect { status { isOk() } }
            options(team.outsider, team.eventId).andExpect { status { isForbidden() } }
            mockMvc.get("/api/v2/events/${team.eventId}/options").andExpect { status { isUnauthorized() } }
            mockMvc.post("/api/v2/events/${team.eventId}/options") {
                contentType = MediaType.APPLICATION_JSON
                content = json(body)
            }.andExpect { status { isUnauthorized() } }
            postOption(team.manager, other.eventId, body).andExpect { status { isForbidden() } }
            options(team.manager, other.eventId).andExpect { status { isForbidden() } }
            postOption(superAdmin(), team.eventId, body).andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("옵션 설명 50자 (#752)")
    inner class DescriptionLength {

        private fun v1Option(team: Team, description: String): Long =
            mockMvc.post("/api/v1/events/${team.eventId}/ticketOptions") {
                with(auth(team.manager))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("type" to "Y/N", "name" to "v1옵션", "description" to description, "additionalPrice" to 0))
            }.andExpect { status { isOk() } }.data().at("/optionGroupId").asLong()

        @Test
        fun `생성(O-2)은 앞뒤 공백 제외 50자까지, 51자는 Option_Group_400_6 (수정과 같은 기준)`() {
            val team = Team()
            fun body(description: String) = mapOf("name" to "옵션", "description" to description, "type" to "YES_NO")
            postOption(team.manager, team.eventId, body("가".repeat(50))).andExpect {
                status { isOk() }
                jsonPath("$.data.description") { value("가".repeat(50)) }
            }
            postOption(team.manager, team.eventId, body("가".repeat(51))).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Option_Group_400_6") }
            }
            postOption(team.manager, team.eventId, body("  ${"가".repeat(50)}  ")).andExpect {
                status { isOk() }
                jsonPath("$.data.description") { value("가".repeat(50)) }
            }
            postOption(team.manager, team.eventId, body("가".repeat(256))).andExpect { status { isBadRequest() } }
            options(team.guest, team.eventId).andExpect { jsonPath("$.data.length()") { value(2) } }
        }

        @Test
        fun `수정(O-3)은 바뀐 설명만 50자 검증 - 51자 Option_Group_400_6, 50자 통과`() {
            val team = Team()
            val option = createOption(team.manager, team.eventId)
            patchOption(team.manager, team.eventId, option, mapOf("description" to "나".repeat(51))).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Option_Group_400_6") }
            }
            patchOption(team.manager, team.eventId, option, mapOf("description" to "나".repeat(50))).andExpect {
                status { isOk() }
                jsonPath("$.data.description") { value("나".repeat(50)) }
            }
            // 컬럼 길이(255)를 넘으면 요청 검증에서 400
            patchOption(team.manager, team.eventId, option, mapOf("description" to "나".repeat(256))).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `v1 에서 만든 50자 넘는 설명 - v1 은 그대로 허용, v2 수정은 설명을 그대로 보내면 통과하고 바꾸면 400`() {
            val team = Team()
            val long = "다".repeat(60)
            val option = v1Option(team, long)
            // 폼 전체를 다시 보내는 수정 화면: 설명은 그대로(앞뒤 공백 무시), 이름만 바꿈
            patchOption(team.manager, team.eventId, option, mapOf("name" to "새 이름", "description" to " $long ")).andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("새 이름") }
                jsonPath("$.data.description") { value(long) }
            }
            patchOption(team.manager, team.eventId, option, mapOf("description" to long + "라")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Option_Group_400_6") }
            }
            options(team.guest, team.eventId).andExpect { jsonPath("$.data[0].description") { value(long) } }
        }
    }

    @Nested
    @DisplayName("O-5 티켓 옵션 전체 지정")
    inner class Apply {

        @Test
        fun `전체 지정·중복 제거·빈 배열 떼기, 다른 공연 옵션 400, 없는 옵션 404, 무료티켓 유료옵션 400, 일반 403`() {
            val team = Team()
            val other = Team("다른호스트")
            val a = createOption(team.manager, team.eventId, yesAdditionalPrice = 1000, name = "A")
            val b = createOption(team.manager, team.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "B")
            val otherOption = createOption(other.manager, other.eventId)
            val ticket = createTicket(team.manager, team.eventId)
            val free = createTicket(team.manager, team.eventId, freeBody())

            putOptions(team.manager, team.eventId, ticket, listOf(b, a, a)).andExpect {
                status { isOk() }
                jsonPath("$.data.options.length()") { value(2) }
                jsonPath("$.data.options[0].optionId") { value(a) }
                jsonPath("$.data.options[0].yesAdditionalPrice") { value(1000) }
                jsonPath("$.data.options[1].optionId") { value(b) }
            }
            putOptions(team.manager, team.eventId, ticket, listOf(b)).andExpect { jsonPath("$.data.options.length()") { value(1) } }
            putOptions(team.manager, team.eventId, ticket, listOf(b, otherOption)).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Option_Group_400_1") }
            }
            putOptions(team.manager, team.eventId, ticket, listOf(987654321L)).andExpect { status { isNotFound() } }
            putOptions(team.manager, team.eventId, free, listOf(a)).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Item_Option_Group_400_4") }
            }
            putOptions(team.guest, team.eventId, ticket, emptyList()).andExpect { status { isForbidden() } }
            putOptions(team.manager, team.eventId, ticket, emptyList()).andExpect { jsonPath("$.data.options.length()") { value(0) } }
            // 실패한 요청은 반영되지 않는다
            manage(team.guest, team.eventId).andExpect { jsonPath("$.data[1].options.length()") { value(0) } }
        }
    }

    @Nested
    @DisplayName("판매된 티켓 잠금 (DEC-012) / O-3 수정 / O-4 삭제")
    inner class Lock {

        @Test
        fun `v1 사용자가 옵션 답변과 함께 구매하면 잠김 - 이름·설명만 수정, 추가금 변경·삭제·옵션 변경 400`() {
            val team = Team()
            val yesNo = createOption(team.manager, team.eventId, yesAdditionalPrice = 2000, name = "뒷풀이")
            val subjective = createOption(team.manager, team.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "입금자명")
            val ticket = createTicket(team.manager, team.eventId, dudoongBody(supplyCount = 10))
            putOptions(team.manager, team.eventId, ticket, listOf(yesNo, subjective)).andExpect { status { isOk() } }
            setEventStatus(team.eventId, EventStatus.OPEN)

            // v1 옵션 조회에도 보인다
            mockMvc.get("/api/v1/events/${team.eventId}/ticketItems/$ticket/options") { with(auth(team.outsider)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.optionGroups.length()") { value(2) }
                jsonPath("$.data.optionGroups[0].type") { value("Y/N") }
                jsonPath("$.data.optionGroups[0].options.length()") { value(2) }
                jsonPath("$.data.optionGroups[1].type") { value("주관식") }
            }
            v1Buy(newUser("구매자"), team.master, team.eventId, ticket, approval = true)

            options(team.guest, team.eventId).andExpect {
                jsonPath("$.data[0].isLocked") { value(true) }
                jsonPath("$.data[1].isLocked") { value(true) }
            }
            patchOption(team.manager, team.eventId, yesNo, mapOf("name" to "뒷풀이 참석", "description" to "새 설명", "yesAdditionalPrice" to 2000)).andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("뒷풀이 참석") }
                jsonPath("$.data.description") { value("새 설명") }
            }
            patchOption(team.manager, team.eventId, yesNo, mapOf("yesAdditionalPrice" to 3000)).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Option_Group_400_5") }
            }
            deleteOption(team.manager, team.eventId, yesNo).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Option_Group_400_2") }
            }
            putOptions(team.manager, team.eventId, ticket, listOf(yesNo)).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Item_Option_Group_400_2") }
            }
            // 같은 목록은 허용 (순서 무관)
            putOptions(team.manager, team.eventId, ticket, listOf(subjective, yesNo)).andExpect { status { isOk() } }
            options(team.guest, team.eventId).andExpect { jsonPath("$.data[0].yesAdditionalPrice") { value(2000) } }
        }

        @Test
        fun `판매 전 티켓에만 붙으면 전체 수정, 삭제하면 티켓에서도 떼어진다`() {
            val team = Team()
            val option = createOption(team.manager, team.eventId, yesAdditionalPrice = 0)
            val ticket = createTicket(team.manager, team.eventId)
            val free = createTicket(team.manager, team.eventId, freeBody())
            putOptions(team.manager, team.eventId, ticket, listOf(option)).andExpect { status { isOk() } }
            putOptions(team.manager, team.eventId, free, listOf(option)).andExpect { status { isOk() } }

            // 무료티켓에 붙어 있으면 유료로 못 바꾼다
            patchOption(team.manager, team.eventId, option, mapOf("yesAdditionalPrice" to 500))
                .andExpect { status { isBadRequest() }; jsonPath("$.code") { value("Item_Option_Group_400_4") } }
            putOptions(team.manager, team.eventId, free, emptyList()).andExpect { status { isOk() } }
            patchOption(team.manager, team.eventId, option, mapOf("yesAdditionalPrice" to 500, "name" to "변경")).andExpect {
                status { isOk() }
                jsonPath("$.data.yesAdditionalPrice") { value(500) }
                jsonPath("$.data.name") { value("변경") }
            }
            patchOption(team.manager, team.eventId, option, mapOf("name" to "  ")).andExpect { status { isBadRequest() } }
            patchOption(team.guest, team.eventId, option, mapOf("name" to "x")).andExpect { status { isForbidden() } }

            deleteOption(team.guest, team.eventId, option).andExpect { status { isForbidden() } }
            deleteOption(team.manager, team.eventId, option).andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(0) }
            }
            manage(team.guest, team.eventId).andExpect { jsonPath("$.data[0].options.length()") { value(0) } }
            deleteOption(team.manager, team.eventId, option).andExpect { status { isNotFound() } }
            // 삭제된 티켓 옵션은 v1 조회에도 없다
            mockMvc.get("/api/v1/events/${team.eventId}/ticketItems/$ticket/options") { with(auth(team.guest)) }
                .andExpect { jsonPath("$.data.optionGroups.length()") { value(0) } }
        }

        @Test
        fun `다른 공연 옵션 id 를 내 공연 경로로 수정·삭제하면 404, 남의 공연 경로는 403`() {
            val team = Team()
            val other = Team("다른호스트")
            val otherOption = createOption(other.manager, other.eventId)
            patchOption(team.manager, team.eventId, otherOption, mapOf("name" to "탈취")).andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("Option_Group_404_1") }
            }
            deleteOption(team.manager, team.eventId, otherOption).andExpect { status { isNotFound() } }
            patchOption(team.manager, other.eventId, otherOption, mapOf("name" to "탈취")).andExpect { status { isForbidden() } }
            options(other.guest, other.eventId).andExpect { jsonPath("$.data[0].name") { value("뒷풀이") } }
        }
    }
}
