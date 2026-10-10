FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

ENV GRADLE_USER_HOME=/root/.gradle


COPY . .
RUN chmod +x gradlew

# Share downloaded Gradle dependencies between Java services.
RUN --mount=type=cache,id=tickethub-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew :statistic-service:bootJar --no-daemon --console=plain -x test

RUN JAR_FILE=$(find statistic-service/build/libs \
    -type f \
    -name "*.jar" \
    ! -name "*-plain.jar" \
    | head -n 1) \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

