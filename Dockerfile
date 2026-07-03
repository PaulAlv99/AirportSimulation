FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25

WORKDIR /app
COPY --from=build /app/target/airport-simulation-cli-1.0.0.jar /app/airport-simulation-cli.jar
VOLUME ["/app/data", "/app/logs", "/app/saves"]

ENTRYPOINT ["java", "-jar", "/app/airport-simulation-cli.jar"]
