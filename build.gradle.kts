import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.spring") version "1.9.22" apply false
    kotlin("plugin.jpa") version "1.9.22" apply false
    kotlin("kapt") version "1.9.22" apply false
    kotlin("plugin.allopen") version "1.9.22" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

tasks.bootJar { enabled = false }

repositories {
    mavenCentral()
}

subprojects {
    group = "band.gosrock"
    version = "0.0.1-SNAPSHOT"

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // JPA 엔티티 클래스를 open으로 만들어 Hibernate 프록시 및 Mockito 서브클래스 목킹 지원
    configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
        annotation("jakarta.persistence.MappedSuperclass")
    }

    // KAPT adds -proc:none to compileTestJava, breaking Lombok annotation processing
    tasks.withType<JavaCompile> {
        if (name == "compileTestJava") {
            doFirst {
                options.compilerArgs.remove("-proc:none")
            }
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            // 타입 인자 어노테이션(List<@Valid X> 등)을 바이트코드에 남긴다 — Bean Validation 컨테이너 원소 검증용 (#718).
            // Kotlin 1.9 는 기본으로 남기지 않아 중첩 검증이 조용히 빠졌다. 적용 시점 기준 main 소스에서 이 형태는 v2 주문 DTO 뿐(v1 영향 없음)
            freeCompilerArgs.add("-Xemit-jvm-type-annotations")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // Kotlin 기본 의존성
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        testImplementation("org.springframework.boot:spring-boot-starter-test")

        // Lombok (Java 테스트 파일에서 사용 - 추후 테스트 Kotlin 전환 시 제거)
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
    }

    tasks.test {
        useJUnitPlatform()
        // Api 통합 테스트는 컨텍스트 여러 개(설정별 캐시) + 공유 H2 를 한 JVM 에 올린다. 기본 512MB 로는 #719 테스트 추가 후 OOM (2026-10-05)
        maxHeapSize = "1g"
        // 비밀값은 yml 기본값이 없다 (#764). 테스트 전용 값을 넣는다 (운영 값 아님). 같은 이름의 환경변수보다 우선한다
        systemProperty("JWT_SECRET_KEY", "testkeytestkeytestkeytestkeytestkeytestkeytestkeytestkeytestkey")
        systemProperty("TOSS_PAYMENTS_KEY", "test_sk_ADpexMgkW36weAqp4bNVGbR5ozO0")
        systemProperty("TOSS_MID", "gosroc9mwo")
        systemProperty("AWS_ACCESS_KEY", "test-access-key")
        systemProperty("AWS_SECRET_KEY", "test-secret-key")
    }
}

ktlint {
    version.set("0.50.0")
    android.set(false)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
