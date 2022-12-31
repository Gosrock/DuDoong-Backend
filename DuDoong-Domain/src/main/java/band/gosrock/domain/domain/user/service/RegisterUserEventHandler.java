package band.gosrock.domain.domain.user.service;


import band.gosrock.domain.common.events.user.UserRegisterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterUserEventHandler {
    @Async
    @TransactionalEventListener(
            classes = UserRegisterEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleteUserEvent(UserRegisterEvent userRegisterEvent) {
        Long userId = userRegisterEvent.getUserId();
        log.info(userId.toString() + "유저 등록");
    }
}
