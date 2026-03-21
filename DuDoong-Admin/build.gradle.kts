tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation(project(":DuDoong-Domain"))
    implementation(project(":DuDoong-Common"))
    implementation("org.apache.poi:poi:5.2.0")
    implementation("org.apache.poi:poi-ooxml:5.2.0")
}
