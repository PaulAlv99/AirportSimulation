FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY data ./data
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/airport-simulation-1.0.0.jar /app/airport-simulation.jar
COPY --from=build /app/data ./data
VOLUME ["/app/data", "/app/logs", "/app/saves"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/airport-simulation.jar"]
