plugins {
    alias(libs.plugins.spring.boot) // используем алиас из Version Catalog
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(platform(libs.testcontainers.bom)) // BOM для Testcontainers

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    runtimeOnly(libs.postgresql)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainersPostgresql)
    // testImplementation(libs.testcontainersJunitJupiter) не работает
    testImplementation(libs.testcontainers.junit.jupiter) // работает нормально

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass = "ru.taranenkoant.banking.account.AccountServiceApplication"
}