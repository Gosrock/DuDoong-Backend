package band.gosrock.domain.common.util;


import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** QueryDsl 에서 compileQuerydsl 빌드를 통해 생성된 클래스 객체 타입을 받아 Sort 의 대상이 되는 Q타입 클래스 객체 리스트를 전달 */
public class QueryDslUtil {
    public static <T> OrderSpecifier[] getOrderSpecifiers(
            Class<? extends T> type, Pageable pageable) {
        final String variable = type.getSimpleName().toLowerCase();
        final List<OrderSpecifier> orderSpecifiers = new ArrayList<>();
        final PathBuilder<T> entityPath = new PathBuilder<>(type, variable);
        for (Sort.Order order : pageable.getSort()) {
            // 존재하지 않는 필드명 검사 - 필드에 없으면 무시됨
            if (hasField(type, order.getProperty())) {
                PathBuilder<Object> path = entityPath.get(order.getProperty());
                orderSpecifiers.add(
                        new OrderSpecifier(Order.valueOf(order.getDirection().name()), path));
            }
        }
        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }

    private static <T> Boolean hasField(Class<? extends T> type, String name) {
        return Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.getName().equals(name));
    }
}
