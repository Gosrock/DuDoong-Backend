dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":DuDoong-Domain"))
    implementation(project(":DuDoong-Common"))
    implementation(project(":DuDoong-Infrastructure"))
    implementation(project(":DuDoong-Admin"))
}
