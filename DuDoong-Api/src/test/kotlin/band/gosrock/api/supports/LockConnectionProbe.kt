package band.gosrock.api.supports

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 스레드별로 지금 쥔 JDBC 커넥션 수를 센다 (#734 리뷰 M-1, #743). 풀 전체 활성 수는 다른 스레드(알림 executor 등)가 섞여 흔들린다.
 * MockMvc 요청·락 트랜잭션·REQUIRES_NEW 는 모두 테스트 스레드에서 돈다.
 * [windows] 는 열린 측정 구간(락 획득 ~ 해제 등)마다 그 안에서 본 최댓값을 기록한다
 */
object ThreadConnections {
    private val counts = ThreadLocal.withInitial { IntArray(2) }
    private val windows = ThreadLocal.withInitial { ArrayDeque<IntArray>() }

    val open: Int get() = counts.get()[0]

    val peak: Int get() = counts.get()[1]

    fun reset() = counts.get().fill(0)

    fun opened() {
        val c = counts.get()
        c[0]++
        c[1] = maxOf(c[1], c[0])
        windows.get().forEach { it[0] = maxOf(it[0], c[0]) }
    }

    fun closed() {
        counts.get()[0]--
    }

    /** 구간 시작: 지금 열린 수를 시작 최댓값으로 */
    fun beginWindow() = windows.get().addLast(intArrayOf(open))

    /** 구간 끝: 구간 안 최댓값 */
    fun endWindow(): Int = windows.get().removeLastOrNull()?.get(0) ?: -1
}

/** DataSource 를 감싸 [ThreadConnections] 를 센다 */
class CountingDataSource(target: DataSource) : DelegatingDataSource(target) {
    override fun getConnection(): Connection = track(super.getConnection())

    override fun getConnection(username: String, password: String): Connection = track(super.getConnection(username, password))

    private fun track(connection: Connection): Connection {
        ThreadConnections.opened()
        var closed = false
        return Proxy.newProxyInstance(Connection::class.java.classLoader, arrayOf(Connection::class.java)) { _, method, args ->
            if (method.name == "close" && !closed) {
                closed = true
                ThreadConnections.closed()
            }
            try {
                method.invoke(connection, *(args ?: emptyArray()))
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        } as Connection
    }
}

/**
 * `@RedissonLock` 메서드별 커넥션 측정 (#743). 메서드마다 세 값의 최댓값을 모은다:
 * - entry: 락 AOP·트랜잭션 AOP 보다 바깥(호출 측이 이미 쥔 수)
 * - waiting: 락을 얻은 직후, 락 트랜잭션(`CallTransaction.proceed`)을 열기 전에 쥔 수 = 락을 기다리는 동안 쥔 수 (그 사이 커넥션을 열 코드가 없다)
 * - callerTx: 락 메서드를 부를 때 이미 진행 중이던 트랜잭션 이름(호출 측 트랜잭션 — 그 커넥션을 쥔 채 락을 기다린다)
 * - inside: 락 트랜잭션 구간(`CallTransaction.proceed`, 락 획득 ~ 해제 안)의 최댓값 (락 트랜잭션 + 그 안의 REQUIRES_NEW 등)
 *
 * DataSource 는 [LockConnectionProbePostProcessor] 가 감싼다. 환경변수 `LOCK_PROBE_OUT`(파일 경로)이 있으면 JVM 종료 때 그 파일에 TSV 로 쓴다 — 예: `LOCK_PROBE_OUT=/tmp/lock.tsv ./gradlew :DuDoong-Api:test --rerun`
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
class LockConnectionProbe {

    data class Stat(var calls: Int = 0, var entry: Int = 0, var waiting: Int = 0, var inside: Int = 0, val callerTx: MutableSet<String> = sortedSetOf())

    @Around("@annotation(band.gosrock.domain.common.aop.redissonLock.RedissonLock)")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val name = "${method.declaringClass.simpleName}.${method.name}"
        current.get().addLast(name)
        val callerTx = if (TransactionSynchronizationManager.isActualTransactionActive()) TransactionSynchronizationManager.getCurrentTransactionName() ?: "?" else null
        stat(name).also { synchronized(it) { it.calls++; it.entry = maxOf(it.entry, ThreadConnections.open); callerTx?.let(it.callerTx::add) } }
        try {
            return joinPoint.proceed()
        } finally {
            current.get().removeLastOrNull()
        }
    }

    @Around("execution(* band.gosrock.domain.common.aop.redissonLock.CallTransaction+.proceed(..))")
    fun aroundLockTransaction(joinPoint: ProceedingJoinPoint): Any? {
        val name = current.get().lastOrNull() ?: return joinPoint.proceed()
        stat(name).also { synchronized(it) { it.waiting = maxOf(it.waiting, ThreadConnections.open) } }
        ThreadConnections.beginWindow()
        try {
            return joinPoint.proceed()
        } finally {
            val inside = ThreadConnections.endWindow()
            stat(name).also { synchronized(it) { it.inside = maxOf(it.inside, inside) } }
        }
    }

    companion object {
        private val current = ThreadLocal.withInitial { ArrayDeque<String>() }
        val stats = ConcurrentHashMap<String, Stat>()

        private fun stat(name: String) = stats.computeIfAbsent(name) { Stat() }

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    val path = System.getenv("LOCK_PROBE_OUT") ?: return@Thread
                    if (stats.isEmpty()) return@Thread
                    val out = File(path)
                    out.parentFile?.mkdirs()
                    out.writeText(
                        "method\tcalls\tentry\twaiting\tinside\tcallerTx\n" +
                            stats.toSortedMap().entries.joinToString("\n") { (k, v) ->
                                "$k\t${v.calls}\t${v.entry}\t${v.waiting}\t${v.inside}\t${v.callerTx.joinToString(",") { it.substringAfterLast('.', it).let { m -> it.substringBeforeLast('.').substringAfterLast('.') + "." + m } }}"
                            } + "\n",
                    )
                },
            )
        }
    }
}

/** DataSource 를 [CountingDataSource] 로 감싼다. 모든 통합 테스트 컨텍스트에 적용 ([ApiIntegrateTestConfig]) */
class LockConnectionProbePostProcessor : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any =
        if (bean is DataSource && bean !is CountingDataSource) CountingDataSource(bean) else bean
}
