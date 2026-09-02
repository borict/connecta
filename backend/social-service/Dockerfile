# Build the jar first, from backend/:
#   mvn -pl social-service -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/social-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
