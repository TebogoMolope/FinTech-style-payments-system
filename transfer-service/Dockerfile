# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy POMs and download dependencies
COPY pom.xml .
COPY transfer-service/pom.xml ./transfer-service/
COPY ledger-service/pom.xml ./ledger-service/
RUN mvn dependency:go-offline

# Copy source code and build the application
COPY . .
RUN mvn -pl transfer-service -am package -DskipTests

# Package stage
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/transfer-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
