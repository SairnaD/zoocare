FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --no-create-home zoocare

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

USER zoocare

EXPOSE 3500

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]