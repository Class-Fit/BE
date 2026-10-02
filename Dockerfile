# =========================
# 1. Build Stage
# =========================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle 관련 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여
RUN chmod +x gradlew

# 의존성 캐싱
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# Spring Boot 실행 JAR 생성
RUN ./gradlew bootJar --no-daemon -x test


# =========================
# 2. Runtime Stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 단계에서 생성한 jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]