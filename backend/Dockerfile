# --- build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# --- run ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 proof
COPY --from=build /src/build/libs/*.jar app.jar
USER proof
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
