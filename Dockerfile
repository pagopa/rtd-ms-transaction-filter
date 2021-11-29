FROM adoptopenjdk/openjdk11:alpine-jre

WORKDIR /app

COPY target/*.jar /app/app.jar
COPY ops_resources/example_config/application_hbsql.yml /app/config.yml
COPY entrypoint.sh /app/entrypoint.sh

RUN mkdir -p /app_workdir/input && \
	mkdir -p /app_workdir/output && \
	mkdir -p /app_workdir/logs && \
	mkdir -p /app_workdir/hpans

RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
