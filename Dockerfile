# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper + settings
COPY gradlew gradlew.bat settings.gradle.kts ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy only the backend module (multi-module safe)
COPY backend backend

# Build fat jar for the backend module
RUN ./gradlew :backend:shadowJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
ENV PORT=8080
WORKDIR /app

# Copy the fat jar
COPY --from=build /workspace/backend/build/libs/backend-all.jar /app/app.jar

# Cloud Run will pass $PORT; your main() already binds 0.0.0.0:$PORT
EXPOSE 8080
CMD ["java","-jar","/app/app.jar"]
