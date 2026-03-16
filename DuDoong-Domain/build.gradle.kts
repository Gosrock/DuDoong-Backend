tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
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
}

// QueryDSL Q클래스 생성 경로 (KAPT 전환 - src/main/generated 제거)
tasks.clean {
    doLast {
        file("src/main/generated").deleteRecursively()
    }
}
