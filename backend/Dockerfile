FROM eclipse-temurin:17-jre-alpine

# Ρύθμιση για σωστά fonts και ελληνικά (προαιρετικό αλλά βοηθάει σε μερικές βιβλιοθήκες)
RUN apk add --no-cache fontconfig ttf-dejavu

EXPOSE 8080

# Αντιγραφή του αρχείου
COPY shadow.jar /app.jar

# Χρησιμοποιούμε flags που βοηθούν τη Java να καταλαβαίνει τα όρια μνήμης του Docker Container
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app.jar"]