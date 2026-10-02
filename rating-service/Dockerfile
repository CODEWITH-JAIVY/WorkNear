# Shared Dockerfile for ALL Maven services.
# Build context = repo root (so the parent pom.xml is available).
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build
COPY pom.xml ./pom.xml
WORKDIR /build/${SERVICE}
COPY ${SERVICE}/pom.xml .
COPY ${SERVICE}/src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]