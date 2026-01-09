# Stage 1: Cache Gradle dependencies
FROM eclipse-temurin:25-jdk-alpine-3.23 AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/app/
COPY gradle /home/gradle/app/gradle
COPY gradlew /home/gradle/app/gradlew
COPY buildSrc /home/gradle/app/buildSrc
COPY lib /home/gradle/app/lib
COPY cli/build.gradle.kts /home/gradle/app/cli/
COPY server/build.gradle.kts /home/gradle/app/server/
WORKDIR /home/gradle/app
RUN ./gradlew dependencies --no-daemon -Pversion=docker-build

# Stage 2: Build UI
FROM node:25-alpine AS ui-build
WORKDIR /app/ui
COPY ui/package*.json ./
RUN npm ci
COPY ui ./
RUN npm run build

# Stage 3: Build Application
FROM eclipse-temurin:25-jdk-alpine-3.23 AS build
ARG VERSION=docker-build
COPY --from=cache /home/gradle/cache_home /root/.gradle
COPY build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/src/
COPY gradle /home/gradle/src/gradle
COPY gradlew /home/gradle/src/gradlew
COPY buildSrc /home/gradle/src/buildSrc
COPY lib /home/gradle/src/lib
COPY cli /home/gradle/src/cli
COPY server /home/gradle/src/server
COPY --from=ui-build /app/ui/dist /home/gradle/src/server/src/main/resources/static
WORKDIR /home/gradle/src
RUN ./gradlew :server:buildFatJar --no-daemon -Pversion=${VERSION}

# Stage 4: Create the Runtime Image
FROM eclipse-temurin:25-jre-alpine-3.23 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*.jar /app/spha-server.jar
ENTRYPOINT ["java","-jar","/app/spha-server.jar"]