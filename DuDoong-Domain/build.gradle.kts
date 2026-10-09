tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("com.mysql:mysql-connector-j")
    // H2 는 테스트에서만 쓴다 — 운영 jar 에 넣지 않는다 (#764). Domain 을 쓰는 모듈의 테스트도 각자 testRuntimeOnly 로 넣는다
    testRuntimeOnly("com.h2database:h2")
    implementation(project(":DuDoong-Common"))
    implementation(project(":DuDoong-Infrastructure"))

    // QueryDSL (KAPT - Kotlin/Java 모든 엔티티의 Q클래스 생성)
    api("com.querydsl:querydsl-core")
    api("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")

    // for @Nullable
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // v1/v2 경계 아키텍처 테스트 (DEC-018)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

// QueryDSL Q클래스 생성 경로 (KAPT 전환 - src/main/generated 제거)
tasks.clean {
    doLast {
        file("src/main/generated").deleteRecursively()
    }
}
