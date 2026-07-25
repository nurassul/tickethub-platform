FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew \
    && ./gradlew :event-service:bootJar --no-daemon -x test \
    && find event-service/build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} /app/event-service.jar \;

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/event-service.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]