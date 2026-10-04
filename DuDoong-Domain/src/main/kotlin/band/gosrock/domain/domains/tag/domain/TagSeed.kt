package band.gosrock.domain.domains.tag.domain

/**
 * 초기 태그 목록 (DEC-014). db/migration/V002 의 [DATA] 시드와 같은 값이어야 한다.
 * 분류 안 순서가 sortOrder 다.
 */
object TagSeed {
    val DEFAULT: List<Pair<TagCategory, List<String>>> = listOf(
        TagCategory.EVENT_TYPE to listOf("정기공연", "연합공연", "시리즈공연", "단독공연", "페스티벌", "쇼케이스", "연주회"),
        TagCategory.GENRE to listOf("락밴드", "어쿠스틱·포크", "클래식", "K-pop", "J-pop", "힙합", "메탈"),
        TagCategory.AREA to listOf("홍대", "합정", "신촌", "강남"),
        TagCategory.TEAM to listOf("대학밴드", "직장인밴드", "실용음악과", "인디"),
    )
}
