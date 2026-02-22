FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml                         ./pom.xml
COPY authentification/pom.xml        ./authentification/pom.xml
COPY notification/pom.xml            ./notification/pom.xml
COPY backend-for-frontend/pom.xml    ./backend-for-frontend/pom.xml
COPY rent-receipt/pom.xml            ./rent-receipt/pom.xml
COPY messaging/pom.xml               ./messaging/pom.xml

COPY backend-for-frontend/src        ./backend-for-frontend/src

RUN apk add --no-cache maven && \
    mvn -pl backend-for-frontend -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/backend-for-frontend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
