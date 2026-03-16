dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    api("org.springframework.boot:spring-boot-starter-aop")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }
