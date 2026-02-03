FROM eclipse-temurin:17-jre-alpine

EXPOSE 8080

# Τώρα το αρχείο είναι στον ίδιο φάκελο, οπότε δεν χρειάζεται το build/libs/

COPY shadow.jar /app.jar

ENTRYPOINT ["java", "-Xmx512m", "-jar", "/app.jar"]