package band.gosrock.common.exception

class NotAvailableRedissonLockException private constructor() : DuDoongCodeException(GlobalErrorCode.NOT_AVAILABLE_REDISSON_LOCK) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = NotAvailableRedissonLockException()
    }
}
