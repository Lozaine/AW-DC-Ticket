# ===== Build Stage =====
# Use a maintained Maven + JDK image
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven wrapper files (optional, for projects using ./mvnw)
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B || ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests || ./mvnw clean package -DskipTests


# ===== Runtime Stage =====
# Use a lightweight JRE image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Run with memory optimization for Railway
CMD ["java", "-Xmx512m", "-Xms256m", "-Djava.awt.headless=true", "-jar", "app.jar"]
