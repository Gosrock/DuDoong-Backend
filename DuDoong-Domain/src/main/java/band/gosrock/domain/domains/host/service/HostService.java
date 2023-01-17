package band.gosrock.domain.domains.host.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.host.repository.HostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HostService {
    private final HostRepository hostRepository;


}
