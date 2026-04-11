# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper & pom.xml trước để tận dụng Docker layer cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cache layer riêng)
RUN ./mvnw dependency:go-offline -B

# Copy source code và build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Tạo user non-root để bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Đổi owner file về non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose port Spring Boot
EXPOSE 8080

# Chạy ứng dụng với profile mặc định (override khi chạy container)
ENTRYPOINT ["java", "-jar", "app.jar"]
