package band.gosrock.domain;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.function.Executable;

/** 동시성 테스트를 편하게 하라고 친철히 만든.. 동시성용 util 클래스 - 이찬진 */
@Slf4j
public class CunCurrencyExecutorService {
    static int numberOfThreads = 10;
    static int numberOfThreadPool = 5;

    public static void execute(Executable executable, AtomicLong successCount)
            throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreadPool);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (long i = 1; i <= numberOfThreads; i++) {
            executorService.submit(
                    () -> {
                        try {
                            executable.execute();
                            // 오류없이 성공을 하면 성공횟수를 증가시킵니다.
                            successCount.getAndIncrement();
                        } catch (Throwable e) {
                            // 에러뜨면 여기서 확인해보셔요!
                            log.info(e.getClass().getName());
                        } finally {
                            latch.countDown();
                        }
                    });
        }
        latch.await();
    }
}
