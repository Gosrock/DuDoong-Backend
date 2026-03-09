package band.gosrock.domain.common.util

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.PathBuilder
import org.springframework.data.domain.Pageable

/** QueryDsl 에서 compileQuerydsl 빌드를 통해 생성된 클래스 객체 타입을 받아 Sort 의 대상이 되는 Q타입 클래스 객체 리스트를 전달 */
object QueryDslUtil {
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> getOrderSpecifiers(type: Class<out T>, pageable: Pageable): Array<OrderSpecifier<*>> {
        val variable = type.simpleName.lowercase()
        val orderSpecifiers = mutableListOf<OrderSpecifier<*>>()
        val entityPath = PathBuilder(type, variable)
        for (order in pageable.sort) {
            if (hasField(type, order.property)) {
                val path = entityPath.get(order.property) as PathBuilder<Comparable<Any>>
                orderSpecifiers.add(OrderSpecifier(Order.valueOf(order.direction.name), path))
            }
        }
        return orderSpecifiers.toTypedArray()
    }

    private fun <T> hasField(type: Class<out T>, name: String): Boolean =
        type.declaredFields.any { it.name == name }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getSortedColumn(order: Order, parent: Path<*>, fieldName: String): OrderSpecifier<*> {
        val fieldPath = Expressions.path(Any::class.java, parent, fieldName) as com.querydsl.core.types.dsl.SimpleExpression<Comparable<Any>>
        return OrderSpecifier(order, fieldPath)
    }
}
