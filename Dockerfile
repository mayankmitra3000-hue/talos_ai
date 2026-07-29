# ---------- Build stage ----------
FROM eclipse-temurin:26-jdk AS build

WORKDIR /app

# Copy Maven wrapper files first
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Give execution permission to Maven wrapper
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# ---------- Run stage ----------
FROM eclipse-temurin:26-jre

WORKDIR /app

# Copy generated JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Render's default web-service port
EXPOSE 10000

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]