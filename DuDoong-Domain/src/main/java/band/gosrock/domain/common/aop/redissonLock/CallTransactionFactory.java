package band.gosrock.domain.common.aop.redissonLock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CallTransactionFactory {

    private final RedissonCallSameTransaction redissonCallSameTransaction;
    private final RedissonCallNewTransaction redissonCallNewTransaction;

    public CallTransaction getCallTransaction(boolean needNew){
        if(needNew == true){
            return redissonCallNewTransaction;
        }
        return redissonCallSameTransaction;
    }
}
