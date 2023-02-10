package band.gosrock.domain.domains.order.repository.condition;

import static band.gosrock.domain.domains.user.domain.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.function.Function;

/** 어드민 테이블의 검색어 지정 ( 전화번호 이름 검색 ) 을 지원하기 위함. */
public enum AdminTableSearchType {
    // 검색 형식은 지원... 저장 형식이 이럼 xxxx-xxxx
    PHONE(user.profile.phoneNumberVo.phoneNumber::contains),
    NAME(user.profile.name::contains);

    private final Function<String, BooleanExpression> expression;

    AdminTableSearchType(Function<String, BooleanExpression> expression) {
        this.expression = expression;
    }

    public BooleanExpression getContains(String keyWord) {
        return expression.apply(keyWord);
    }
}
