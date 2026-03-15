package band.gosrock.domain.common.alarm

object OrderKakaoTalkAlarm {
    @JvmStatic
    fun creationOf(userName: String, hostName: String, eventName: String): String =
        "안녕하세요 ${userName}님!\n" +
            "$hostName $eventName" +
            "티켓이 발급됐습니다.\n" +
            "\n" +
            "원활한 입장을 위해 QR코드를 미리 준비해주세요."

    @JvmStatic
    fun creationHeaderOf(): String = "주문 완료 안내"

    @JvmStatic
    fun creationTemplateCode(): String = "doneorderv2"

    @JvmStatic
    fun deletionOf(userName: String, hostName: String, eventName: String): String =
        "안녕하세요 ${userName}님!\n$hostName $eventName 주문이 취소되어 안내드립니다.\n"

    @JvmStatic
    fun deletionHeaderOf(): String = "주문 취소 안내"

    @JvmStatic
    fun deletionTemplateCode(): String = "cancelorder"
}
