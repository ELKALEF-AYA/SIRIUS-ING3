# ============================================================
# Dockerfile – rent-receipt service
# Context: project ROOT (SIRIUS-ING3/)
# Build:  docker build -f rent-receipt/Dockerfile -t rent-receipt-service .
# ============================================================

# ---------- Stage 1 : Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy the parent POM + the shared messaging module + this service
COPY pom.xml                ./pom.xml
COPY messaging/pom.xml      ./messaging/pom.xml
COPY messaging/src          ./messaging/src
COPY rent-receipt/pom.xml   ./rent-receipt/pom.xml
COPY rent-receipt/src       ./rent-receipt/src

RUN apk add --no-cache maven && \
    mvn -pl messaging,rent-receipt -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

# ---------- Stage 2 : Runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/rent-receipt/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
