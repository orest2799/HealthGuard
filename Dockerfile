# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper + settings
COPY gradlew gradlew.bat settings.gradle.kts ./
COPY gradle gradle

# Ensure gradlew is executable (and fix CRLF if copied from Windows)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy project sources (multi-module safe)
COPY backend backend

# Build backend only (skip tests to speed up)
RUN ./gradlew :backend:installDist --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
ENV PORT=8080
WORKDIR /app

# Copy self-contained distribution produced by installDist
COPY --from=build /workspace/backend/build/install/backend /app

# Cloud Run will call $PORT; Ktor must bind 0.0.0.0:$PORT (your main() already does)
EXPOSE 8080
CMD ["./bin/backend"]
