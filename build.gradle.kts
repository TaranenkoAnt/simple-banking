plugins {
    //  общие плагины для всех модулей
    id("java")
    // apply false для Spring Boot и Dependency Management — эти плагины мы будем подключать только в тех модулях, где они нужны
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
}

// allprojects задаёт группу и версию для всех модулей и добавляет репозиторий Maven Central
allprojects {
    group = "ru.taranenkoant.banking"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// subprojects применяет плагин java ко всем подмодулям, настраивает Java 17 и JUnit 5
subprojects {
    apply(plugin = "java")
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}