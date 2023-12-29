# Используйте мавен для сборки вашего приложения
FROM maven:3.8.4-openjdk-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
RUN mvn clean package

# Используйте базовый образ Java для запуска собранного jar
FROM eclipse-temurin:21-jre
WORKDIR /app
# Копируем только jar из сборочного контейнера
COPY --from=build /app/target/telegram-bot-chatgpt.jar /app/telegram-bot-chatgpt.jar

# Открываем порт, если ваше приложение использует веб-сервер
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "telegram-bot-chatgpt.jar"]
