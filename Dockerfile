# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/FIIS-bot-1.0-SNAPSHOT.jar app.jar

# Run as non-root user for security
RUN addgroup -S botuser && adduser -S botuser -G botuser
USER botuser

# Environment variables (set via platform, not hardcoded)
# TELEGRAM_BOT_TOKEN=
# TELEGRAM_BOT_USERNAME=

ENTRYPOINT ["java", "-jar", "app.jar"]
