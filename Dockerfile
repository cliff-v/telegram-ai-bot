# Использование базового образа с Maven
FROM maven:3.6.3-jdk-11 AS build

# Копирование исходного кода
COPY src /home/app/src
COPY pom.xml /home/app

# Сборка приложения
RUN mvn -f /home/app/pom.xml clean package \
    \
FROM eclipse-temurin:17 as app-build
ENV RELEASE=17

WORKDIR /opt/build
COPY ./target/telegram-bot-chatgpt.jar ./application.jar

RUN java -Djarmode=layertools -jar application.jar extract
RUN $JAVA_HOME/bin/jlink \
         --add-modules `jdeps --ignore-missing-deps -q -recursive --multi-release ${RELEASE} --print-module-deps -cp 'dependencies/BOOT-INF/lib/*' application.jar`,jdk.crypto.cryptoki \
         --strip-java-debug-attributes \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output jdk

FROM debian:buster-slim

ARG BUILD_PATH=/opt/build
ENV JAVA_HOME=/opt/jdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"

RUN apt update && groupadd --gid 1000 spring-app \
  && useradd --uid 1000 --gid spring-app --shell /bin/bash --create-home spring-app

USER spring-app:spring-app
WORKDIR /opt/workspace

COPY --from=app-build $BUILD_PATH/jdk $JAVA_HOME
COPY --from=app-build $BUILD_PATH/spring-boot-loader/ ./
COPY --from=app-build $BUILD_PATH/dependencies/ ./
COPY --from=app-build $BUILD_PATH/application/ ./

ENTRYPOINT ["java", "-Dfile.encoding=UTF8", "-Dconsole.encoding=UTF8", "org.springframework.boot.loader.JarLauncher"]