package band.gosrock.domain.common.util

import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumberInstance {
    @JvmField
    val instance: PhoneNumberUtil = PhoneNumberUtil.getInstance()
}
