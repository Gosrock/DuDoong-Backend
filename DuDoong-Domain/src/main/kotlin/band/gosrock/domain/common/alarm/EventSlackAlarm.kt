package band.gosrock.domain.common.alarm

import band.gosrock.domain.domains.event.domain.Event

object EventSlackAlarm {
    @JvmStatic
    fun creationOf(eventName: String): String = "새로운 공연 '$eventName'이(가) 열렸습니다!"

    @JvmStatic
    fun changeStatusOf(event: Event): String = "${nameOf(event)}의 상태가 ${statusOf(event)}으로 변경되었습니다."

    @JvmStatic
    fun changeContentOf(event: Event): String = "${nameOf(event)}의 내용이 변경되었습니다. 확인해주세요!"

    @JvmStatic
    fun deletionOf(eventName: String): String = "'$eventName' 공연이 삭제되었습니다."

    private fun nameOf(event: Event): String = "'${event.eventBasic?.name}'"

    private fun statusOf(event: Event): String = "'${event.status?.value}'"
}
