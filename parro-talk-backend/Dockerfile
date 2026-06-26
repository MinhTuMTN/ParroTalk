# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# ============================================================
# Stage 2: Extract layers từ JAR
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS extractor

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Use layertools to extract
RUN java -Djarmode=layertools -jar app.jar extract

# ============================================================
# Stage 3: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=extractor --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=extractor --chown=appuser:appgroup /app/snapshot-dependencies/ ./

COPY --from=extractor --chown=appuser:appgroup /app/application/ ./

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]