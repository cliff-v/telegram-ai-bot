# Используйте мавен для сборки вашего приложения
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
ENV TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
ENV TELEGRAM_BOT_USERNAME:${TELEGRAM_BOT_USERNAME}
ENV TELEGRAM_PERSON_ID:${TELEGRAM_PERSON_ID}
ENV ADMIN_TELEGRAM_PERSON_ID:${ADMIN_TELEGRAM_PERSON_ID}
ENV CHAT_GPT_TOKEN:${CHAT_GPT_TOKEN}
RUN mvn clean package

# Используйте базовый образ Java для запуска собранного jar
FROM eclipse-temurin:17-jre
WORKDIR /app
# Копируем только jar из сборочного контейнера
COPY --from=build /app/target/telegram-bot-chatgpt.jar /app/telegram-bot-chatgpt.jar

# Открываем порт, если ваше приложение использует веб-сервер
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "telegram-bot-chatgpt.jar"]
