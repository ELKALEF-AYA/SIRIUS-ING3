FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml                         ./pom.xml
COPY authentification/pom.xml        ./authentification/pom.xml
COPY notification/pom.xml            ./notification/pom.xml
COPY backend-for-frontend/pom.xml    ./backend-for-frontend/pom.xml
COPY rent-receipt/pom.xml            ./rent-receipt/pom.xml
COPY messaging/pom.xml               ./messaging/pom.xml

COPY messaging/src                   ./messaging/src
COPY notification/src                ./notification/src

RUN apk add --no-cache maven && \
    mvn -pl messaging,notification -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/notification/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
