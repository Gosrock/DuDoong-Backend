package band.gosrock.domain.common.vo

import band.gosrock.domain.common.util.PhoneNumberInstance
import band.gosrock.domain.domains.user.exception.UserPhoneNumberInvalidException
import com.fasterxml.jackson.annotation.JsonValue
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import jakarta.persistence.Embeddable

@Embeddable
class PhoneNumberVo() {
    // +82 10-xxxx-xxxx format 으로 저장.
    var phoneNumber: String? = null
        protected set

    constructor(rawPhoneNumber: String?) : this() {
        this.phoneNumber = rawPhoneNumber
    }

    companion object {
        @JvmStatic
        fun valueOf(rawPhoneNumber: String?): PhoneNumberVo = PhoneNumberVo(rawPhoneNumber)

        private fun getPhoneNumber(rawPhoneNumber: String): PhoneNumber {
            return try {
                PhoneNumberInstance.instance.parse(rawPhoneNumber, "KR")
            } catch (e: NumberParseException) {
                throw UserPhoneNumberInvalidException.EXCEPTION
            }
        }
    }

    /** 010-xxxx-xxxx format */
    @JsonValue
    fun getNationalFormat(): String {
        val num = getPhoneNumber(phoneNumber ?: return "")
        return PhoneNumberInstance.instance.format(num, PhoneNumberFormat.NATIONAL)
    }

    /** +82 10-xxxx-xxxx format */
    @Throws(NumberParseException::class)
    fun getInternationalFormat(): String {
        val num = getPhoneNumber(phoneNumber ?: return "")
        return PhoneNumberInstance.instance.format(num, PhoneNumberFormat.INTERNATIONAL)
    }

    /** 01000000000 format */
    @Throws(NumberParseException::class)
    fun getNaverSmsToNumber(): String = getNationalFormat().replace("-", "")
}
