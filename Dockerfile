# ===== Build Stage =====
FROM ghcr.io/lozaine/maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for caching
COPY pom.xml .

# Download deps
RUN mvn dependency:go-offline -B

# Copy sources
COPY src ./src

# Build
RUN mvn clean package -DskipTests

# ===== Runtime Stage =====
FROM ghcr.io/lozaine/eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /app/target/AWDCTicket-1.3.0.jar app.jar

# Expose default web port (Railway will map $PORT)
EXPOSE 8080

# Environment variables for web dashboard
ENV PUBLIC_BASE_URL=${PUBLIC_BASE_URL:-https://aw-dc-ticket-production.up.railway.app}
ENV BOT_CLIENT_ID=${BOT_CLIENT_ID}
ENV BOT_CLIENT_SECRET=${BOT_CLIENT_SECRET}

CMD ["sh", "-c", "java -Xmx512m -Xms256m -Djava.awt.headless=true -jar app.jar"]
