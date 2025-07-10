FROM eclipse-temurin:21-jdk

WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "build/libs/motoo-0.0.1-SNAPSHOT.jar", "--spring.config.location=file:/app/config/application.yml"]
