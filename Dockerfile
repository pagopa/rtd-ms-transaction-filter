FROM openjdk:8-jdk-alpine
VOLUME /tmp
RUN mkdir /tmp/input && mkdir /tmp/output && mkdir /tmp/logs && mkdir /tmp/hpans
WORKDIR /app
COPY target/*.jar /app/app.jar
COPY ops_resources/example_config/application_hbsql.yml /app/config.yml
ENTRYPOINT ["sleep", "600"]
