plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.4"))

    implementation(project(":tradecore-core"))
    implementation(project(":tradecore-db"))
//    implementation(project(":tradecore-infra"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework:spring-tx")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}