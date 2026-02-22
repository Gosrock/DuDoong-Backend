import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot") version "2.7.7"
    id("io.spring.dependency-management") version "1.0.15.RELEASE" apply false
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.spring") version "1.9.22" apply false
    kotlin("plugin.jpa") version "1.9.22" apply false
    kotlin("kapt") version "1.9.22" apply false
    id("com.diffplug.spotless") version "6.11.0"
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
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }

    // KAPT adds -proc:none to compileJava to suppress Java annotation processing,
    // but Java sources (Lombok) still need annotation processing during compileJava.
    tasks.withType<JavaCompile> {
        doFirst {
            options.compilerArgs.remove("-proc:none")
        }
    }

    // compileOnly extends annotationProcessor so Lombok annotations are visible at compile time
    configurations {
        compileOnly {
            extendsFrom(configurations["annotationProcessor"])
        }
    }

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.8"
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.test)
        reports {
            html.required.set(true)
            csv.required.set(true)
            xml.required.set(true)
            xml.outputLocation.set(file("${buildDir}/reports/jacoco.xml"))
        }

        val qDomains = ('A'..'Z').map { "**/Q$it*" }

        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it) {
                            exclude(
                                "**/*Application*",
                                "**/*Config*",
                                "**/*Dto*",
                                "**/*Request*",
                                "**/*Response*",
                                "**/*Interceptor*",
                                "**/*Exception*",
                            )
                            exclude(qDomains)
                        }
                    },
                ),
            )
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // Kotlin 기본 의존성
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        // Lombok (Java 소스 Kotlin 마이그레이션 완료 전까지 유지)
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy("jacocoTestReport")
    }
}

spotless {
    java {
        target("**/*.java")
        googleJavaFormat().aosp()
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        target("**/*.kt")
        ktlint("0.50.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
