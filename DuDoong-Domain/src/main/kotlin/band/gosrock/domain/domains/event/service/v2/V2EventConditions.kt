package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.domain.QEvent.event
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import java.time.LocalDateTime

/**
 * v2 사용자 앱 공연 상태 쿼리 조건 — 표시 상태([V2EventDisplayRule])와 같은 기준을 SQL 로 옮긴 것 (#716, #729).
 * 공연 탐색(P-1·P-2), 마이페이지 관심 호스트(M-4)·아카이빙(M-5)이 같이 쓴다
 */
object V2EventConditions {

    /**
     * 종료 전 등록 공연(UPCOMING + ONGOING) = OPEN + (startAt + runTime분) > now.
     * 종료 시각 식은 종료 배치(`EventCustomRepositoryImpl.endAtBefore`)와 같은 TIMESTAMPADD, runTime 이 없으면 0분(종료 = 시작).
     * startAt 이 없으면 NULL → 제외(PAST).
     * start_at 하한 보조 조건(now - 최대 runTime)은 두지 않는다: runTime 상한이 없고(prod 최대 30,000분) OPEN 행이 적어(prod 32건)
     * (status, start_at) 인덱스의 status 동등 조건만으로 OPEN 행을 읽고 종료 식으로 거른다 (V006 주석 EXPLAIN)
     */
    fun active(now: LocalDateTime): BooleanExpression =
        event.status.eq(EventStatus.OPEN).and(endAt().gt(now))

    /**
     * 지난 공개 공연(PAST) = CALCULATING·CLOSED, 또는 OPEN 중 종료 시각이 지났거나 startAt 이 없는 공연 ([V2EventDisplayRule] 과 같음).
     * 준비중·삭제는 포함하지 않는다
     */
    fun ended(now: LocalDateTime): BooleanExpression =
        event.status.`in`(EventStatus.CALCULATING, EventStatus.CLOSED)
            .or(event.status.eq(EventStatus.OPEN).and(event.eventBasic.startAt.isNull.or(endAt().loe(now))))

    private fun endAt() =
        Expressions.dateTimeTemplate(
            LocalDateTime::class.java,
            "TIMESTAMPADD(MINUTE, {0}, {1})",
            event.eventBasic.runTime.coalesce(0L),
            event.eventBasic.startAt,
        )
}
