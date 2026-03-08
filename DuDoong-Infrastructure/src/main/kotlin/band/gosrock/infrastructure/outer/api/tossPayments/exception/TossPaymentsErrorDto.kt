package band.gosrock.infrastructure.outer.api.tossPayments.exception

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Response
import java.io.IOException

class TossPaymentsErrorDto {
    var code: String? = null
    var message: String? = null

    companion object {
        @JvmStatic
        fun from(response: Response): TossPaymentsErrorDto {
            return try {
                response.body().asInputStream().use { bodyIs ->
                    ObjectMapper().readValue(bodyIs, TossPaymentsErrorDto::class.java)
                }
            } catch (e: IOException) {
                throw PaymentsUnHandleException.EXCEPTION
            }
        }
    }
}
