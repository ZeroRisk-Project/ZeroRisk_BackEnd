FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
COPY .env .env
EXPOSE 8081
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]