dependencies {
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation(project(":DuDoong-Domain"))
    implementation(project(":DuDoong-Common"))
    implementation(project(":DuDoong-Infrastructure"))
    testImplementation("org.springframework.batch:spring-batch-test")
    // v1/v2 경계 아키텍처 테스트 (DEC-018)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    implementation("org.apache.poi:poi:5.2.0")
    implementation("org.apache.poi:poi-ooxml:5.2.0")
}
