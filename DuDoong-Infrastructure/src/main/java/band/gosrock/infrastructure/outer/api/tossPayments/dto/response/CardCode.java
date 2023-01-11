package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CardCode {
    IBK_BC("3K", "기업비씨", "IBK_BC", "기업 비씨"),
    GWANGJUBANK("46", "광주", "GWANGJUBANK", "광주은행"),
    LOTTE("71", "롯데", "LOTTE", "롯데카드"),
    KDBBANK("30", "산업", "KDBBANK", "KDB산업은행"),

    BC("31", "", "BC", "비씨카드"),
    SAMSUNG("51", "삼성", "SAMSUNG", "삼성카드"),
    SAEMAUL("38", "새마을", "SAEMAUL", "새마을금고"),
    SHINHAN("41", "신한", "SHINHAN", "신한카드"),
    SHINHYEOP("62", "신협", "SHINHYEOP", "신협"),
    CITI("36", "씨티", "CITI", "씨티카드"),
    WOORI("33", "우리", "WOORI", "우리카드"),
    POST("37", "우체국", "POST", "우체국예금보험"),
    SAVINGBANK("39", "저축", "SAVINGBANK", "저축은행중앙회"),
    JEONBUKBANK("35", "전북", "JEONBUKBANK", "전북은행"),
    JEJUBANK("42", "제주", "JEJUBANK", "제주은행"),
    KAKAOBANK("15", "카카오뱅크", "KAKAOBANK", "카카오뱅크"),
    KBANK("3A", "케이뱅크", "KBANK", "케이뱅크"),
    TOSSBANK("24", "토스뱅크", "TOSSBANK", "토스뱅크"),
    HANA("21", "하나", "HANA", "하나카드"),
    HYUNDAI("61", "현대", "HYUNDAI", "현대카드"),
    KOOKMIN("11", "국민", "KOOKMIN", "KB국민카드"),
    NONGHYEOP("91", "농협", "NONGHYEOP", "NH농협카드"),
    SUHYEOP("34", "수협", "SUHYEOP", "Sh수협은행"),
    // 해외
    DINERS("6D", "다이너스", "DINERS", "다이너스 클럽"),
    DISCOVER("6I", "디스커버", "DISCOVER", "디스커버"),
    MASTER("4M", "마스터", "MASTER", "마스터카드"),
    UNIONPAY("3C", "유니온페이", "UNIONPAY", "유니온페이"),
    AMEX("7A", "", "AMEX", "아메리칸 익스프레스"),
    JCB("4J", "", "JCB", "JCB"),
    VISA("4V", "비자", "VISA", "VISA");

    private String code;
    private String kr;
    private String en;
    private String cardCompanyName;
}
