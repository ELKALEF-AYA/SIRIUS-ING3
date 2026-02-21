# ============================================================
# Dockerfile – backend-for-frontend (BFF) service
# Context: project ROOT (SIRIUS-ING3/)
# Build:  docker build -f backend-for-frontend/Dockerfile -t bff-service .
# ============================================================

# ---------- Stage 1 : Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy the parent POM + this service only
COPY pom.xml                        ./pom.xml
COPY backend-for-frontend/pom.xml   ./backend-for-frontend/pom.xml
COPY backend-for-frontend/src       ./backend-for-frontend/src

RUN apk add --no-cache maven && \
    mvn -pl backend-for-frontend -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

# ---------- Stage 2 : Runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/backend-for-frontend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
