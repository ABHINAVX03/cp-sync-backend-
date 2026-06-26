# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/cpsync-backend-*.jar app.jar

EXPOSE 8080

# FIXED: JVM memory tuning for 512MB container
# MaxRAMPercentage=75 → uses ~384MB of 512MB for heap
# ZGC → low-pause GC, better for latency-sensitive workloads
# urandom → faster SecureRandom startup
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseZGC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

# FIXED: Docker health check so orchestrator knows if the app crashes post-startup
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1