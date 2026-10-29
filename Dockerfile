FROM gradle:8.6-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :SulckBackend/ktor-app:shadowJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/SulckBackend/ktor-app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]