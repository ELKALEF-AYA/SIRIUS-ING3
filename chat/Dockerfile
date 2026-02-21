# ============================================================
# Dockerfile – chat service (standalone Spring Boot project)
# Context: chat/
# Build:  docker build -t chat-service ./chat
# ============================================================

# ---------- Stage 1 : Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy Maven wrapper + pom
COPY pom.xml    .
COPY src        ./src

# Build (skip tests)
RUN apk add --no-cache maven && \
    mvn -DskipTests=true --no-transfer-progress package

# ---------- Stage 2 : Runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
