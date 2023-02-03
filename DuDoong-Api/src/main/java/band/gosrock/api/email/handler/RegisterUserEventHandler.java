package band.gosrock.api.email.handler;


import band.gosrock.api.email.service.SendRegisterEmailService;
import band.gosrock.domain.common.events.user.UserRegisterEvent;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
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

    private final UserAdaptor userAdaptor;

    private final SendRegisterEmailService sendRegisterEmailService;

    @Async
    @TransactionalEventListener(
            classes = UserRegisterEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegisterUserEvent(UserRegisterEvent userRegisterEvent) {
        Long userId = userRegisterEvent.getUserId();
        User user = userAdaptor.queryUser(userId);
        log.info(userId.toString() + "유저 등록");
        Profile profile = user.getProfile();
        sendRegisterEmailService.execute(profile.getName(), profile.getEmail());
    }
}
