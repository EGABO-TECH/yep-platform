# Build stage: compile the jar with Maven (no dependencies are downloaded besides Maven plugins).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package

# Run stage: a small Java runtime with the jar and the web folder.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/yep.jar ./yep.jar
COPY web ./web
RUN useradd --system --no-create-home yep
USER yep
ENV PORT=8080
EXPOSE 8080
# MaxRAMPercentage keeps the JVM inside small container memory limits (e.g. Render's free 512 MB).
CMD ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-jar", "yep.jar"]
