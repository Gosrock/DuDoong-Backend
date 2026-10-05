package band.gosrock.api.config

/**
 * 요청 본문 로그·Slack 오류 알림용 개인정보 마스킹 (#718 리뷰).
 * JSON 본문에서 민감 키의 **스칼라 값**(문자열·숫자·불리언)을 `"***"` 로 바꾼다. 키 이름 기준이라 v1·v2 경로 모두 적용되고,
 * 바뀌는 것은 로그·알림 내용뿐이다(요청 처리에는 영향 없음). 잘린 본문(로그 상한)이나 깨진 JSON 에서도 동작하도록 정규식으로 처리한다.
 *
 * 대상 키(대소문자 무시): 계좌(bankName·accountHolder·accountNumber, v2 티켓 계좌 bank·holder·number), 입금자명, 연락처(phoneNumber·email·contactValue·value),
 * 선물 메모(memo — 받는 사람 이름 등을 적는 칸, #719).
 * 경로: 선물 링크 토큰(`/api/v2/gifts/{token}`, #719)은 링크를 가진 사람이 받으므로 요청 로그·Slack 의 URL 에서 [maskPath] 로 가린다
 */
object SensitiveBodyMasker {

    const val MASK = "***"

    val SENSITIVE_KEYS: Set<String> = setOf(
        "accountNumber", "accountHolder", "bankName", "bank", "holder", "number",
        "depositorName", "phoneNumber", "email", "contactValue", "value", "memo",
    )

    /** `/api/v2/gifts/{token}`(뒤에 `/accept`·`/reject` 가 붙어도) 의 토큰 */
    private val GIFT_TOKEN_PATH = Regex("(/api/v2/gifts/)[^/?#]+")

    // "key" : "문자열"(잘려서 닫는 따옴표가 없어도) | 숫자 | true/false
    private val PATTERN = Regex(
        "(\"(?:${SENSITIVE_KEYS.joinToString("|") { Regex.escape(it) }})\"\\s*:\\s*)" +
            "(\"(?:[^\"\\\\]|\\\\.)*(?:\"|$)|-?\\d[\\d.eE+-]*|true|false)",
        RegexOption.IGNORE_CASE,
    )

    fun mask(body: String?): String? = body?.let { PATTERN.replace(it) { m -> "${m.groupValues[1]}\"$MASK\"" } }

    /** 요청 경로·URL 의 선물 토큰을 가린다 (그 밖 경로는 그대로) */
    fun maskPath(path: String): String = GIFT_TOKEN_PATH.replace(path) { m -> "${m.groupValues[1]}$MASK" }
}
