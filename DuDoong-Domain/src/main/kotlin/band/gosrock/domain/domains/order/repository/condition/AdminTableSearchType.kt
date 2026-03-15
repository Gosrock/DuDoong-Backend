package band.gosrock.domain.domains.order.repository.condition

import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.dsl.BooleanExpression

/** 어드민 테이블의 검색어 지정 ( 전화번호 이름 검색 ) 을 지원하기 위함. */
enum class AdminTableSearchType(
    private val expression: (String) -> BooleanExpression
) {
    // 검색 형식은 지원... 저장 형식이 이럼 xxxx-xxxx
    PHONE({ keyword -> user.profile.phoneNumberVo.phoneNumber.contains(keyword) }),
    NAME({ keyword -> user.profile.name.contains(keyword) });

    fun getContains(keyWord: String): BooleanExpression = expression(keyWord)
}
