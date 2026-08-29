FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY . .
RUN chmod +x gradlew

RUN ./gradlew :event-service:bootJar --no-daemon -x test

RUN JAR_FILE=$(find event-service/build/libs \
    -type f \
    -name "*.jar" \
    ! -name "*-plain.jar" \
    | head -n 1) \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar
EXPOSE 8084
ENV SERVER_PORT=8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
