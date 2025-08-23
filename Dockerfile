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
COPY --from=build /app/target/AWDCTicket-1.0-SNAPSHOT.jar app.jar

CMD ["java", "-Xmx512m", "-Xms256m", "-Djava.awt.headless=true", "-jar", "app.jar"]
