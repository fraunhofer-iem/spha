# Use a multi-stage build for smaller final image size
# Stage 1: Build the application
FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /app

# Copy only necessary files for dependency resolution and build
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY src src
# Needed to calculate the semantic version
COPY .git .git


# Build the application
RUN ./gradlew installDist

# Stage 2: Create the production image
FROM eclipse-temurin:24-jre-alpine


WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/build /app/build

# Set the entrypoint and default command
ENTRYPOINT ["/app/build/install/spha-cli/bin/spha-cli"]
CMD ["--help"]
