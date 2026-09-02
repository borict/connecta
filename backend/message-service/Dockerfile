# Build the jar first, from backend/:
#   mvn -pl message-service -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/message-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
