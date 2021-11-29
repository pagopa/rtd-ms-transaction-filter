FROM adoptopenjdk/openjdk11:alpine-jre

WORKDIR /app

COPY target/*.jar /app/app.jar
COPY ops_resources/example_config/application_hbsql.yml /app/config.yml
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
