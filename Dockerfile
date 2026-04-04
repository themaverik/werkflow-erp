# ================================================================
# WERKFLOW ERP — BUSINESS SERVICE STANDALONE BUILD
# ================================================================
# This Dockerfile builds the Business Service for standalone deployment
# as an external optional capability service.
# ================================================================

# ================================================================
# STAGE 1: Backend Base - Build werkflow-common and werkflow-delegates
# ================================================================
FROM maven:3.9-eclipse-temurin-21 AS backend-base

WORKDIR /build

# Copy and build werkflow-common first (required by other services)
COPY shared/common/pom.xml shared/common/pom.xml
COPY shared/common/src shared/common/src

RUN cd shared/common && mvn clean install -DskipTests -B

# Copy and build werkflow-delegates (required by other services)
COPY shared/delegates/pom.xml shared/delegates/pom.xml
COPY shared/delegates/src shared/delegates/src

# Build and install werkflow-delegates to local maven repo
RUN cd shared/delegates && mvn clean install -DskipTests -B

# Now copy business service pom
COPY services/business/pom.xml services/business/pom.xml

# Download dependencies for business service (werkflow-common and werkflow-delegates now available locally)
RUN cd services/business && mvn dependency:go-offline -B

# ================================================================
# STAGE 2: Business Service Build
# ================================================================
FROM backend-base AS business-service-build

WORKDIR /build/services/business

# Copy source code
COPY services/business/src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================================================
# STAGE 3: Business Service Runtime
# ================================================================
FROM eclipse-temurin:21-jre AS business-service

# Add non-root user
RUN groupadd -r werkflow && useradd -r -g werkflow werkflow

WORKDIR /app

# Copy JAR from build stage
COPY --from=business-service-build /build/services/business/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/logs && \
    chown -R werkflow:werkflow /app

USER werkflow

EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "-jar", \
    "app.jar"]
