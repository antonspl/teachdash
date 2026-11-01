FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home teachdash
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER teachdash
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
