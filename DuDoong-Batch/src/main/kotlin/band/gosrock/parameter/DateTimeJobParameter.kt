package band.gosrock.parameter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.beans.factory.annotation.Value

class DateTimeJobParameter {

    private var _time: LocalDateTime? = null

    @Value("#{jobParameters[dateTime]}")
    fun setDateTime(dateTime: String?) {
        // 인자가 없다면 now 로 설정
        if (dateTime == null) {
            this._time = LocalDateTime.now()
        } else {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                this._time = LocalDateTime.parse(dateTime, formatter)
            } catch (e: Exception) {
                throw JobParametersInvalidException("올바르지 않은 시간 형식입니다")
            }
        }
    }

    fun getTime(): LocalDateTime = _time!!
}
