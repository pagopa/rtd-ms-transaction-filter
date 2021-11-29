FROM adoptopenjdk/openjdk11:alpine-jre

VOLUME /app_workdir

WORKDIR /app

COPY target/*.jar /app/app.jar
COPY ops_resources/example_config/application_hbsql.yml /app/config.yml
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
