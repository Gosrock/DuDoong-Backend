tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("mysql:mysql-connector-java")
    runtimeOnly("com.h2database:h2")
    implementation(project(":DuDoong-Common"))
    implementation(project(":DuDoong-Infrastructure"))

    // QueryDSL (Java APT - Kotlin 소스 전환 전까지 유지)
    api("com.querydsl:querydsl-core")
    api("com.querydsl:querydsl-jpa")
    annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jpa")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // for @Nullable
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

// QueryDSL Q클래스 생성 경로
val generated = "src/main/generated"

sourceSets {
    main {
        java.srcDirs(generated)
    }
}

tasks.withType<JavaCompile> {
    options.annotationProcessorGeneratedSourcesDirectory = file(generated)
}

tasks.clean {
    doLast {
        file(generated).deleteRecursively()
    }
}
