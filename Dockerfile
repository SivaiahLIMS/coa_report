# Multi-stage build for Spring Boot backend
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml
COPY backend/pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY backend/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install wget and netcat for healthcheck and wait script
RUN apk add --no-cache wget busybox-extras

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy wait-for-db script
COPY wait-for-db.sh /app/wait-for-db.sh
RUN chmod +x /app/wait-for-db.sh

# Change ownership
RUN chown spring:spring app.jar /app/wait-for-db.sh

# Switch to non-root user
USER spring:spring

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/app/wait-for-db.sh"]
CMD ["java", "-jar", "app.jar"]
