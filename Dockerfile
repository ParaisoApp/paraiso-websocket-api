# Stage 1: Build
FROM gradle:8.6.0-jdk21 AS builder
WORKDIR /app
COPY . /app
RUN gradle :server:fatJar -x test
RUN ls -al /app/server/build/libs   # debug

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/server/build/libs/server-all.jar server.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","server.jar"]
