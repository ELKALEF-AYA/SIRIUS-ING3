# ============================================================
# Dockerfile – authentification service
# Context: project ROOT (SIRIUS-ING3/)
# Build:  docker build -f authentification/Dockerfile -t auth-service .
# ============================================================

# ---------- Stage 1 : Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy the parent POM + this service only
COPY pom.xml                    ./pom.xml
COPY authentification/pom.xml   ./authentification/pom.xml
COPY authentification/src       ./authentification/src

# Build only this module (skip tests)
RUN apk add --no-cache maven && \
    mvn -pl authentification -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

# ---------- Stage 2 : Runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/authentification/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
