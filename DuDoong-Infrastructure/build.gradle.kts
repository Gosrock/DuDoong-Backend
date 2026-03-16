tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    api("com.slack.api:slack-api-client:1.27.2")
    api("io.github.openfeign:feign-httpclient:12.1")
    api("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.0")
    api(project(":DuDoong-Common"))
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.redisson:redisson:3.25.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    api("com.amazonaws:aws-java-sdk-s3control:1.12.372")
    api("io.github.openfeign:feign-jackson:12.1")
    api("com.bucket4j:bucket4j-core:8.1.1")
    api("com.bucket4j:bucket4j-jcache:8.1.1")

    // for email
    api("org.springframework.boot:spring-boot-starter-thymeleaf")
    api("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    api("software.amazon.awssdk:ses:2.19.29")

    api("com.googlecode.libphonenumber:libphonenumber:8.13.5")
    api("org.xhtmlrenderer:flying-saucer-pdf:9.1.20")
    api("com.sun.mail:jakarta.mail:2.0.1")

    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.1.0")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.1.0")
}
