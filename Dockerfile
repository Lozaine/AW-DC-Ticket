# Use a stable Maven image
FROM maven:3.8.6-openjdk-17-slim AS build

WORKDIR /app

# Copy Maven wrapper files (if using wrapper)
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B || ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests || ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/AWDCTicket-1.0-SNAPSHOT.jar app.jar

# Run with memory optimization for Railway
CMD ["java", "-Xmx512m", "-Xms256m", "-Djava.awt.headless=true", "-jar", "app.jar"]