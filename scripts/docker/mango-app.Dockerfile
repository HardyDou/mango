# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-jammy

ARG JAR_FILE
ARG APP_PORT=8080

ENV SPRING_PROFILES_ACTIVE=nacos \
    MANGO_NACOS_SERVER_ADDR=127.0.0.1:8848 \
    MANGO_NACOS_CONFIG_ENABLED=false \
    JAVA_OPTS="" \
    APP_ARGS=""

WORKDIR /opt/mango

RUN groupadd --system mango \
    && useradd --system --gid mango --create-home --home-dir /home/mango --shell /usr/sbin/nologin mango \
    && chown -R mango:mango /opt/mango

COPY ${JAR_FILE} /opt/mango/app.jar

RUN chown mango:mango /opt/mango/app.jar

USER mango
EXPOSE ${APP_PORT}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/mango/app.jar $APP_ARGS"]
