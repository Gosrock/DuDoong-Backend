package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.vo.PhoneNumberVo
import band.gosrock.domain.domains.user.domain.User
import javax.persistence.Embeddable

@Embeddable
class IssuedTicketUserInfoVo() {

    var userId: Long? = null
        protected set

    var userName: String? = null
        protected set

    var phoneNumber: PhoneNumberVo? = null
        protected set

    var email: String? = null
        protected set

    constructor(userId: Long?, userName: String?, email: String?, phoneNumber: PhoneNumberVo?) : this() {
        this.userId = userId
        this.userName = userName
        this.email = email
        this.phoneNumber = phoneNumber
    }

    companion object {
        @JvmStatic
        fun from(user: User): IssuedTicketUserInfoVo = IssuedTicketUserInfoVo(
            userId = user.id,
            userName = user.profile?.name,
            phoneNumber = user.profile?.phoneNumberVo,
            email = user.profile?.email,
        )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        private var userName: String? = null
        private var email: String? = null
        private var phoneNumber: PhoneNumberVo? = null

        fun userId(userId: Long?) = apply { this.userId = userId }
        fun userName(userName: String?) = apply { this.userName = userName }
        fun email(email: String?) = apply { this.email = email }
        fun phoneNumber(phoneNumber: PhoneNumberVo?) = apply { this.phoneNumber = phoneNumber }

        fun build(): IssuedTicketUserInfoVo = IssuedTicketUserInfoVo(userId, userName, email, phoneNumber)
    }
}
