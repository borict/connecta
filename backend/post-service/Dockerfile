# Build the jar first, from backend/:
#   mvn -pl post-service -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/post-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
