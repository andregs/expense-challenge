FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY backend/gradlew ./
COPY backend/gradle ./gradle
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon --configuration runtimeClasspath -q
COPY backend/src ./src
RUN ./gradlew bootJar --no-daemon -x test -q

FROM eclipse-temurin:25-jdk AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
