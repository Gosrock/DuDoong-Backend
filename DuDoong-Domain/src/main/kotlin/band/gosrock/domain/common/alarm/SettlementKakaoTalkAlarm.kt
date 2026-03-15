package band.gosrock.domain.common.alarm

object SettlementKakaoTalkAlarm {
    @JvmStatic
    fun creationOf(hostName: String): String =
        "안녕하세요 ${hostName}님!\n이메일로 정산서 발송이 완료되어 안내드립니다."

    @JvmStatic
    fun creationHeaderOf(): String = "정산서 이메일 발송 안내"

    @JvmStatic
    fun creationTemplateCode(): String = "settlement"
}
