package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminHostResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetHostsUseCase(
    private val hostAdaptor: HostAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, keyword: String?, pageable: Pageable): Page<AdminHostResponse> {
        adminAuthValidator.validateManagerOrAbove(userId)
        return hostAdaptor.findAllForAdmin(keyword, pageable)
            .map { AdminHostResponse.from(it) }
    }
}
