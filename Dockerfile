# Stage 1: Build
FROM gradle:8.3-jdk21 AS builder
WORKDIR /app
COPY . /app
# Build the server module fatJar
RUN gradle :server:fatJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the fatJar built from the server module
COPY --from=builder /app/server/build/libs/server-all.jar server.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","server.jar"]
