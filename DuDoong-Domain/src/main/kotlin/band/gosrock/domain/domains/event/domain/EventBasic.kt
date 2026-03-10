package band.gosrock.domain.domains.event.domain

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class EventBasic() {

    @Column(length = 25)
    var name: String? = null
        protected set

    var startAt: LocalDateTime? = null
        protected set

    var runTime: Long? = null
        protected set

    constructor(name: String?, startAt: LocalDateTime?, runTime: Long?) : this() {
        this.name = name
        this.startAt = startAt
        this.runTime = runTime
    }

    fun isUpdated(): Boolean = this.name != null && this.startAt != null && this.runTime != null

    fun endAt(): LocalDateTime? = if (this.runTime == null) null else this.startAt?.plusMinutes(this.runTime!!)

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var name: String? = null
        private var startAt: LocalDateTime? = null
        private var runTime: Long? = null

        fun name(name: String?) = apply { this.name = name }
        fun startAt(startAt: LocalDateTime?) = apply { this.startAt = startAt }
        fun runTime(runTime: Long?) = apply { this.runTime = runTime }
        fun build() = EventBasic(name, startAt, runTime)
    }
}
