# Build stage: Compile the Spring Boot application using Maven
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY ZapServices/ . 

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage: Run the compiled Spring Boot application
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
