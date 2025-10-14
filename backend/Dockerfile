# -------- Build stage --------
FROM gradle:8.7-jdk17-alpine AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle --no-daemon clean shadowJar

# -------- Runtime stage --------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*-all.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
