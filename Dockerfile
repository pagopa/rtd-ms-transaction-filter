FROM maven:3.9.4-amazoncorretto-17-al2023@sha256:c668a2ee8a376c82977408f57970a996f5d6d3d5f017149d02d396eed2c850b3 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM amazoncorretto:17.0.10-alpine3.19@sha256:b63c613a40c3b939fa4970ed0aa84238a781777c7ad5ef8a42edba4e94847feb AS runtime

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
