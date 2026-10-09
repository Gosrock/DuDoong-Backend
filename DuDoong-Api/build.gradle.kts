dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":DuDoong-Domain"))
    implementation(project(":DuDoong-Common"))
    implementation(project(":DuDoong-Infrastructure"))
    implementation(project(":DuDoong-Admin"))
    // v2 섹션 HTML sanitize (XSS)
    implementation("org.jsoup:jsoup:1.17.2")

    testImplementation("org.springframework.security:spring-security-test")
    // 통합 테스트용 H2. Domain 에서 runtimeOnly 를 뺐다 (#764)
    testRuntimeOnly("com.h2database:h2")
    // v1/v2 경계 아키텍처 테스트 (DEC-018)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    // v2 엑셀 응답 파싱 검증 (#712). 운영 코드는 Admin 모듈의 AdminExcelService 로만 쓴다
    testImplementation("org.apache.poi:poi-ooxml:5.2.0")
}
