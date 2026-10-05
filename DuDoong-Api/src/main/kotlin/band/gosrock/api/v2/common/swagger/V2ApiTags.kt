package band.gosrock.api.v2.common.swagger

/**
 * v2 Swagger 태그 (#731). `[영역] 번호. 이름` — 번호는 와이어프레임 순서(10·11 문서 장 순서)이고 Swagger UI 는 이름순(tagsSorter=alpha)으로 정렬한다.
 * 영역 안에서 번호는 겹치지 않는다(`V2SwaggerGroupsTest`). 태그 설명은 처음 쓰는 컨트롤러 한 곳에만 둔다.
 *
 * 권한 표기: P = 비로그인 허용, U = 로그인, G+ = 호스트 일반 멤버 이상, M+ = 매니저 이상, MS = 마스터만 (SUPER_ADMIN 은 G+/M+/MS 검사를 건너뜀)
 */
object V2ApiTags {
    const val HOST = "[호스팅] 1. 호스트·멤버"
    const val HOST_DESCRIPTION = "H-1~H-15. 내 호스트(U), 호스트 생성(U)·공개 홈(P)·설정 수정(M+), 멤버 목록(G+)·추가/삭제(M+)·역할 변경/마스터 양도(MS), 팔로우(U), 호스트 공연 리스트(P)"

    const val EVENT_PREP = "[호스팅] 2. 공연 준비"
    const val EVENT_PREP_DESCRIPTION = "E-1~E-11. 공연준비 홈(U), 간편 생성·기본 정보·섹션·등록·삭제·이미지(M+), 관리 상세·체크리스트(G+), 섹션·태그 목록(P)"

    const val TICKET = "[호스팅] 3. 티켓"
    const val TICKET_DESCRIPTION = "T-1~T-6, O-5. 관리용 목록(G+), 생성·수정·삭제·판매 중단/재개·옵션 지정(M+). 판매된 티켓은 수정 범위가 제한된다 (DEC-006)"

    const val TICKET_OPTION = "[호스팅] 4. 티켓 옵션"
    const val TICKET_OPTION_DESCRIPTION = "O-1~O-4 (호스팅 센터 문서 기준 — 사용자 앱 O-* 주문과 별개). 옵션 풀(G+), 생성·수정·삭제(M+)"

    const val OPERATION_ORDER = "[호스팅] 5. 공연 운영 - 대시보드·주문·환불"
    const val OPERATION_ORDER_DESCRIPTION = "D-1, R-1~R-6, F-1~F-2. 조회·엑셀(G+), 승인·거절·취소·환불 완료(M+). 환불 계좌는 매니저 이상에게만"

    const val OPERATION_TICKET = "[호스팅] 6. 공연 운영 - 발급 티켓·QR 체크인"
    const val OPERATION_TICKET_DESCRIPTION = "I-1~I-3, Q-1~Q-5. 발급 티켓 조회·엑셀·체크인(G+), 관객 셀프 체크인(U)"

    const val NOTIFICATION = "[호스팅] 7. 알림센터"
    const val NOTIFICATION_DESCRIPTION = "N-1~N-3 (U). 호스팅 센터·사용자 앱(마이페이지 M-6) 공통 — 받는 사람 기준으로 내 알림만"

    const val HEALTH = "[호스팅] 8. 헬스체크"
    const val HEALTH_DESCRIPTION = "v2 경로 동작 확인 (P)"

    const val BROWSE = "[사용자] 1. 공연 탐색"
    const val BROWSE_DESCRIPTION = "P-1~P-5 (P). 홈·공연 리스트·공개 상세·판매 중 티켓. 준비중·삭제 공연은 404, 계좌는 노출하지 않는다"

    const val ORDER = "[사용자] 2. 주문·주문내역·취소"
    const val ORDER_DESCRIPTION = "O-0~O-4 (U). 결제 화면(계좌), 주문 생성, 내 주문 목록·상세, 취소·환불 요청. 본인 주문만 (남의 주문은 404)"

    const val GIFT = "[사용자] 3. 티켓탭·선물"
    const val GIFT_DESCRIPTION = "T-1~T-3 내 티켓·입장 QR, G-1~G-8 선물 링크 생성·취소·받기/거절·반환·내역 (U, 선물 랜딩 G-3 만 P). 사용자 앱 T-* 는 호스팅 센터 티켓 T-* 와 별개"

    const val MYPAGE = "[사용자] 4. 마이페이지"
    const val MYPAGE_DESCRIPTION = "M-1~M-5 (U). 알림센터(M-6)는 '$NOTIFICATION' N-1~N-3(/api/v2/me/notifications) 재사용, " +
        "주문내역은 O-2·O-3, 소속 호스트 전체는 H-1(/api/v2/me/hosts), 언팔로우는 H-13(DELETE /api/v2/hosts/{hostId}/follow), " +
        "로그아웃·탈퇴는 v1(POST /api/v1/auth/logout, DELETE /api/v1/auth/me) 재사용"
}
