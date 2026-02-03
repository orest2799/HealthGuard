# Χρήση Java 17 runtime (Alpine για μικρό μέγεθος)
FROM eclipse-temurin:17-jre-alpine

# Ορισμός θύρας
EXPOSE 8080

# Αντιγραφή του jar που έχουμε ήδη χτίσει τοπικά
# Προσοχή: Το path 'build/libs/shadow.jar' πρέπει να είναι σωστό
COPY build/libs/shadow.jar /app.jar

# Εκκίνηση
ENTRYPOINT ["java", "-Xmx512m", "-jar", "/app.jar"]