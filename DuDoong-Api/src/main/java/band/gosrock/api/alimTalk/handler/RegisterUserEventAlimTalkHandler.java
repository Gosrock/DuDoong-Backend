package band.gosrock.api.alimTalk.handler;


import band.gosrock.api.alimTalk.service.SendRegisterAlimTalkService;
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
public class RegisterUserEventAlimTalkHandler {
    private final UserAdaptor userAdaptor;

    private final SendRegisterAlimTalkService sendRegisterAlimTalkService;

    @Async
    @TransactionalEventListener(
            classes = UserRegisterEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegisterUserEvent(UserRegisterEvent userRegisterEvent) {
        Long userId = userRegisterEvent.getUserId();
        User user = userAdaptor.queryUser(userId);
        log.info(userId.toString() + "유저 등록");
        Profile profile = user.getProfile();
        // TODO: 전화번호 변경 머지되면 바꾸기
        sendRegisterAlimTalkService.execute(profile.getName(), profile.getPhoneNumber());
    }
}
