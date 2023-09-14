FROM maven:3.9.4-amazoncorretto-17-al2023@sha256:c668a2ee8a376c82977408f57970a996f5d6d3d5f017149d02d396eed2c850b3 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM amazoncorretto:17.0.8-alpine3.18@sha256:34650d7c653af234dad21cd2d89d2f0dbdb1bad54041014932e51b3492e0dec5 AS runtime

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
