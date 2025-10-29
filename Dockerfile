# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Gradle wrapper + settings
COPY gradlew gradlew.bat settings.gradle.kts ./
COPY gradle gradle

# Project sources (multi-module safe)
COPY backend backend

# Build only backend app (skip tests to speed up)
RUN ./gradlew :backend:installDist --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
ENV PORT=8080
WORKDIR /app

# Copy self-contained distribution
COPY --from=build /workspace/backend/build/install/backend /app

# Cloud Run hits $PORT; Ktor must bind 0.0.0.0:$PORT
EXPOSE 8080
CMD ["./bin/backend"]
