FROM maven:3.8.4-jdk-11-slim as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM openjdk:19-slim-buster as runtime

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
