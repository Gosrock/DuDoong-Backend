package band.gosrock.common.exception;

public class NotAvailableRedissonLockException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotAvailableRedissonLockException();

    private NotAvailableRedissonLockException() {
        super(ErrorCode.NOT_AVAILABLE_REDISSON_LOCK);
    }
}
