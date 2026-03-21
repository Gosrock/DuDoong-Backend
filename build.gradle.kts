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
