# Build the jar first, from backend/:
#   mvn -pl api-gateway -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
