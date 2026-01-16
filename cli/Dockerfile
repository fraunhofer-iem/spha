# Use a multi-stage build for smaller final image size
# Stage 1: Build the application
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Setup dummy server project to please top level settings.gradle without
# copying unnecessary files
RUN mkdir server && touch server/build.gradle.kts
# Copy only necessary files for dependency resolution and build
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY buildSrc buildSrc
COPY lib lib
COPY cli cli

# Build the application
RUN ./gradlew :cli:installDist

# Stage 2: Create the production image
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/cli/build/install /app

# Set the entrypoint and default command
ENTRYPOINT ["/app/spha-cli/bin/spha-cli"]
CMD ["--help"]
