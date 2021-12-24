FROM maven:3.8.4-jdk-11-slim as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM adoptopenjdk/openjdk11:alpine-jre as runtime

VOLUME /app_workdir
VOLUME /app_certs_in

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
COPY ops_resources/example_config/application_hbsql.yml /app/config.yml
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
