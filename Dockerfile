# Use OpenJDK 17 image
FROM openjdk:17-jdk-slim

# Set work directory
WORKDIR /app

# Copy JAR
COPY target/taskmanager-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run JAR
ENTRYPOINT ["java","-jar","app.jar"]

