package band.gosrock.domain.domains.event.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

/**
 * 공연 상세 정보 섹션 (v2). 순서는 [sortOrder] 오름차순.
 * 제목이 [INTRO_TITLE] 인 첫 섹션이 '공연 소개' 이고, 그 본문이 v1 `tbl_event.content` 와 동기화된다.
 */
@Entity(name = "tbl_event_section")
class EventSection(
    @Column(length = TITLE_MAX_LENGTH, nullable = false)
    var title: String,

    @Column(columnDefinition = "LONGTEXT")
    var content: String? = null,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_section_id")
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event? = null
        protected set

    @Column(nullable = false)
    var sortOrder: Int = 0
        protected set

    fun assignTo(event: Event, sortOrder: Int) {
        this.event = event
        this.sortOrder = sortOrder
    }

    fun isIntro(): Boolean = this.title == INTRO_TITLE

    fun changeContent(content: String?) {
        this.content = content
    }

    companion object {
        /** v1 content 와 동기화되는 섹션 제목 */
        const val INTRO_TITLE = "공연 소개"

        /** 기본 섹션 제목 (프론트 기본 탭). 서버는 강제하지 않는다 */
        val DEFAULT_TITLES = listOf(INTRO_TITLE, "예매안내", "세트리스트", "유의사항")

        const val TITLE_MAX_LENGTH = 20

        // 로컬/dev 는 ddl-auto 로 만든 tbl_event.content 가 TEXT(64KB) 라, 공연 소개 본문이 v1 content 에 들어갈 수 있는 길이로 제한한다
        const val CONTENT_MAX_LENGTH = 20_000
    }
}
