# Use the official OpenJDK image with Gradle pre-installed
FROM gradle:jdk17 AS build

# Set the working directory
WORKDIR /app

# Copy the build files and source code into the container
COPY . .

# Execute the project build
RUN gradle build --no-daemon

# Use a lightweight runtime to run the application
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file from the previous build stage
COPY --from=build /app/build/libs/RouteAnalyzer-1.0-SNAPSHOT-all.jar app.jar

# Cuopy standard files
COPY ./src/main/resources /app/resources

# Command to start the application
CMD ["java", "-jar", "app.jar"]