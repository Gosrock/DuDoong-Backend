package band.gosrock.api.host.handler;


import band.gosrock.api.email.service.HostMasterChangeEmailService;
import band.gosrock.domain.common.events.host.HostUserRoleChangeEvent;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
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
public class HostUserRoleChangeEventHandler {
    private final UserAdaptor userAdaptor;
    private final HostAdaptor hostAdaptor;
    private final HostMasterChangeEmailService hostMasterChangeEmailService;

    @Async
    @TransactionalEventListener(
            classes = HostUserRoleChangeEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HostUserRoleChangeEvent hostUserRoleChangeEvent) {
        final Long userId = hostUserRoleChangeEvent.getUserId();
        final User user = userAdaptor.queryUser(userId);
        final HostRole role = hostUserRoleChangeEvent.getRole();
        final String hostName = hostUserRoleChangeEvent.getHostName();

        // role == master 이면 전체에게 추가 알림 + 이메일
        if (role == HostRole.MASTER) {
            hostMasterChangeEmailService.execute(user.toEmailUserInfo(), hostName, role);
        }
        // todo: userId 본인에게 특정 host 에서의 역할 변경 통보
    }
}
