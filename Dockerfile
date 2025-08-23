FROM maven:3.9.4-openjdk-17-slim AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (shade plugin creates executable jar)
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/AWDCTicket-1.0-SNAPSHOT.jar app.jar

# Run the application (jar is now executable)
CMD ["java", "-jar", "app.jar"]