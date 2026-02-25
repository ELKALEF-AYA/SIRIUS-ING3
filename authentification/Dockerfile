FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml                         ./pom.xml
COPY authentification/pom.xml        ./authentification/pom.xml
COPY notification/pom.xml            ./notification/pom.xml
COPY backend-for-frontend/pom.xml    ./backend-for-frontend/pom.xml
COPY rent-receipt/pom.xml            ./rent-receipt/pom.xml
COPY messaging/pom.xml               ./messaging/pom.xml

COPY authentification/src            ./authentification/src

RUN apk add --no-cache maven && \
    mvn -pl authentification -am \
      -DskipTests=true \
      --no-transfer-progress \
      package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/authentification/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
