FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --home-dir /app --shell /usr/sbin/nologin achabitation
COPY --from=build /workspace/target/achabitation-backend-api-0.1.0-SNAPSHOT.jar /app/achabitation-backend-api.jar
RUN chown -R achabitation:achabitation /app
USER achabitation
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/achabitation-backend-api.jar"]
