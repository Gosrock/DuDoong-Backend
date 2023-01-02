package band.gosrock.api.auth.service;


import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshUseCase {
    private final UserAdaptor userAdaptor;
}
