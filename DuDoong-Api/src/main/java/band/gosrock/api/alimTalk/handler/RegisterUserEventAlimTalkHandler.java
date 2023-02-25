package band.gosrock.api.alimTalk.handler;


import band.gosrock.api.alimTalk.service.SendRegisterAlimTalkService;
import band.gosrock.domain.common.events.user.UserRegisterEvent;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkUserInfo;
import com.google.i18n.phonenumbers.NumberParseException;
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
    public void handleRegisterUserEvent(UserRegisterEvent userRegisterEvent)
            throws NumberParseException {
        Long userId = userRegisterEvent.getUserId();
        User user = userAdaptor.queryUser(userId);
        log.info(userId.toString() + "유저 등록");
        AlimTalkUserInfo userInfo = user.toAlimTalkUserInfo();
        sendRegisterAlimTalkService.execute(userInfo.getUserName(), userInfo.getPhoneNum());
    }
}
