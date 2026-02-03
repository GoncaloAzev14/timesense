FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# RUN apk add krb5
COPY target/*.jar *
COPY target/lib lib

COPY exec.sh .

EXPOSE 8080

ENTRYPOINT ["sh", "exec.sh"]

