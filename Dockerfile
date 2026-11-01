FROM eclipse-temurin:24-jdk AS jdk
FROM debian:trixie-slim
COPY --from=jdk /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates chromium chromium-driver xvfb xauth docker.io fonts-liberation && rm -rf /var/lib/apt/lists/*
ENV CHROME_BIN=/usr/bin/chromium CHROME_DRIVER=/usr/bin/chromedriver
WORKDIR /app
COPY build/libs/softiq-watchdog-app-5.0.0-RELEASE.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]
