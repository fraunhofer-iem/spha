# Stage 1: Cache Gradle dependencies
FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/app/
COPY gradle /home/gradle/app/gradle
COPY gradlew /home/gradle/app/gradlew
RUN chmod +x /home/gradle/app/gradlew && sed -i 's/\r$//' /home/gradle/app/gradlew
COPY buildSrc /home/gradle/app/buildSrc
COPY lib /home/gradle/app/lib
COPY cli/build.gradle.kts /home/gradle/app/cli/
COPY server/build.gradle.kts /home/gradle/app/server/
WORKDIR /home/gradle/app
RUN ./gradlew dependencies --no-daemon -Pversion=docker-build

# Stage 2: Build UI
FROM node:latest AS ui-build
COPY ui /app/ui
WORKDIR /app/ui
RUN npm install
RUN npm run build:demo

# Stage 3: Build Application
FROM gradle:latest AS build
ARG VERSION=docker-build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/src/
COPY --chown=gradle:gradle gradle /home/gradle/src/gradle
COPY --chown=gradle:gradle gradlew /home/gradle/src/gradlew
RUN chmod +x /home/gradle/src/gradlew && sed -i 's/\r$//' /home/gradle/src/gradlew
COPY --chown=gradle:gradle buildSrc /home/gradle/src/buildSrc
COPY --chown=gradle:gradle lib /home/gradle/src/lib
COPY --chown=gradle:gradle cli /home/gradle/src/cli
COPY --chown=gradle:gradle server /home/gradle/src/server
COPY --from=ui-build /app/ui/dist /home/gradle/src/server/src/main/resources/static
WORKDIR /home/gradle/src
RUN ./gradlew :server:buildFatJar --no-daemon -Pversion=${VERSION}

# Stage 4: Create the Runtime Image
FROM amazoncorretto:25 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*.jar /app/spha-server.jar
ENTRYPOINT ["java","-jar","/app/spha-server.jar"]