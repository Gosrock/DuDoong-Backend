package band.gosrock.domain.common.aop.redissonLock

import org.springframework.stereotype.Component

@Component
class CallTransactionFactory(
    private val redissonCallSameTransaction: RedissonCallSameTransaction,
    private val redissonCallNewTransaction: RedissonCallNewTransaction,
) {
    fun getCallTransaction(needSame: Boolean): CallTransaction {
        return if (needSame) redissonCallSameTransaction else redissonCallNewTransaction
    }
}
