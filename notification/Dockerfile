# ============================================================
# Dockerfile – notification service
# Context: project ROOT (SIRIUS-ING3/)
# Build:  docker build -f notification/Dockerfile -t notification-service .
# ============================================================

# ---------- Stage 1 : Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy the parent POM + the shared messaging module + this service
COPY pom.xml                ./pom.xml
COPY messaging/pom.xml      ./messaging/pom.xml
COPY messaging/src          ./messaging/src
COPY notification/pom.xml   ./notification/pom.xml
COPY notification/src       ./notification/src

RUN apk add --no-cache maven && \
    mvn -pl messaging,notification -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

# ---------- Stage 2 : Runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/notification/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
