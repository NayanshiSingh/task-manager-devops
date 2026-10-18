# ==============================================================================
# Task Manager API - Dockerfile
# Multi-stage build for optimized production image
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY checkstyle.xml .

# Build the application (skip tests as they run in CI)
RUN mvn package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

# Traceability: the commit is baked into the image and reported by /actuator/info
ARG GIT_SHA=local
ARG IMAGE_TAG=local
ENV GIT_SHA=${GIT_SHA} IMAGE_TAG=${IMAGE_TAG}
LABEL org.opencontainers.image.source="https://github.com/NayanshiSingh/task-manager-devops" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.title="task-manager"

# Create non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -M -s /usr/sbin/nologin appuser

WORKDIR /app

# Copy the built JAR from builder stage (owned by root, read-only for the app user)
COPY --from=builder /app/target/task-manager-*.jar app.jar

# Switch to non-root user
USER 1001:1001

# Expose port
EXPOSE 8080

# Run the application; heap sized relative to the container memory limit
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.io.tmpdir=/tmp", "-jar", "app.jar"]
