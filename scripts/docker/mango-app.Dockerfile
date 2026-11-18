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

# 中文字体：镜像内置开源 CJK 字体（Noto CJK、文鼎 AR PL UKai 楷体、Liberation）覆盖，
# 业务字体名（宋体/仿宋/楷体/微软雅黑/黑体/小标宋/Calibri 等）由 fontconfig 别名映射到内置字体。
# 字体安装、fc-cache 和覆盖校验都在 root 阶段完成；校验失败即构建失败。
# 运行期禁止再安装字体（应用容器不再具备 apt 安装字体的交付路径）。
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        fonts-noto-cjk \
        fonts-noto-cjk-extra \
        fonts-noto-color-emoji \
        fonts-liberation \
        fonts-crosextra-carlito \
        fonts-crosextra-caladea \
        fonts-arphic-ukai \
    && rm -rf /var/lib/apt/lists/*

COPY scripts/docker/fonts/office-font-aliases.conf /etc/fonts/conf.d/65-mango-office-font-aliases.conf
COPY scripts/docker/fonts/verify-office-fonts.sh /usr/local/bin/verify-office-fonts.sh

RUN chmod 0755 /usr/local/bin/verify-office-fonts.sh \
    && fc-cache -f \
    && /usr/local/bin/verify-office-fonts.sh

RUN groupadd --system mango \
    && useradd --system --gid mango --create-home --home-dir /home/mango --shell /usr/sbin/nologin mango \
    && chown -R mango:mango /opt/mango

COPY ${JAR_FILE} /opt/mango/app.jar

RUN chown mango:mango /opt/mango/app.jar

USER mango
EXPOSE ${APP_PORT}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/mango/app.jar $APP_ARGS"]
