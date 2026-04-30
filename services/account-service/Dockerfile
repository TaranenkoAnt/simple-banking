FROM gradle:8.14-jdk17 AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew gradlew.bat ./
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/libs.versions.toml gradle/

COPY services/account-service/ services/account-service/

RUN ./gradlew :services:account-service:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/services/account-service/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

