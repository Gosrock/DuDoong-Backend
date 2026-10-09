import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot") version "3.2.12"
    id("io.spring.dependency-management") version "1.1.6" apply false
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

    // Boot 3.2.12 기본 Tomcat(10.1.33)은 multipart CVE-2025-48988(10.1.42 수정)·CVE-2025-52520(10.1.43 수정) 이전 버전이다 (#765).
    // 10.1.x 최신 패치로 고정한다. Boot 를 올릴 때 BOM 기본값이 이보다 높아지면 이 줄을 지운다
    extra["tomcat.version"] = "10.1.60"

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
