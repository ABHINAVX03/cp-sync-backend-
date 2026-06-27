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

# ── JVM memory budget for Render free tier (512MB hard limit) ──────────────────
# Heap:              -Xms48m -Xmx192m      → 192MB max heap
# Metaspace cap:     -XX:MaxMetaspaceSize=128m
# Code cache cap:    -XX:ReservedCodeCacheSize=64m
# Class space:       -XX:CompressedClassSpaceSize=32m
# Thread stacks:     -Xss256k (default 512k → halved)
# GC:                SerialGC — lowest overhead for a single-tenant 0.1-CPU container.
# JIT:               TieredStopAtLevel=1 — only the fast-tier compiler runs.
# JMX disabled:      Saves ~10MB of MBean infrastructure we don't use.
#
# Estimated total:   192 + 128 + 64 + 32 + ~30 (native/threads) = ~446MB  ✓
ENTRYPOINT ["java", \
    "-Xms48m", \
    "-Xmx192m", \
    "-XX:+UseSerialGC", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:ReservedCodeCacheSize=64m", \
    "-XX:CompressedClassSpaceSize=32m", \
    "-Xss256k", \
    "-XX:TieredStopAtLevel=1", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.jmx.enabled=false", \
    "-jar", "app.jar"]

# Health check — start-period=90s gives the app time to run Flyway migrations
# and initialise Hibernate before the first check fires.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1