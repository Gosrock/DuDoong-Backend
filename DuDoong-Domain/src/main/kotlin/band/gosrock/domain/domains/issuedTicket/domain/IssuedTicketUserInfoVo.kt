package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.vo.PhoneNumberVo
import band.gosrock.domain.domains.user.domain.User
import jakarta.persistence.Embeddable

@Embeddable
class IssuedTicketUserInfoVo(
    var userId: Long? = null,
    var userName: String? = null,
    var email: String? = null,
    var phoneNumber: PhoneNumberVo? = null,
) {
    companion object {
        @JvmStatic
        fun from(user: User): IssuedTicketUserInfoVo = IssuedTicketUserInfoVo(
            userId = user.id,
            userName = user.profile?.name,
            phoneNumber = user.profile?.phoneNumberVo,
            email = user.profile?.email,
        )
    }
}
