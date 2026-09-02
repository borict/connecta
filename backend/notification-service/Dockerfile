# Build the jar first, from backend/:
#   mvn -pl notification-service -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/notification-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
