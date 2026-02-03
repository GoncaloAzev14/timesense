
FROM alpine:latest

WORKDIR /app

EXPOSE 3000
EXPOSE 8080
EXPOSE 80
EXPOSE 5432

ENV PGDATA=/var/lib/postgresql/data

RUN apk add --no-cache openjdk17 nodejs nginx postgresql postgresql-client \
    && mkdir -p /var/lib/postgresql/data \
    && mkdir -p /run/postgresql \
    && chown -R postgres:postgres /var/lib/postgresql \
    && chown postgres:postgres /run/postgresql \
    && su postgres -c "initdb -D /var/lib/postgresql/data"

COPY target/*.tgz .
COPY dev_environments/standalone/nginx.conf /etc/nginx/http.d/default.conf

RUN mkdir backend \
    && mkdir frontend \
    && tar -xzf *-backend.tgz -C backend \
    && tar -xzf *-frontend.tgz -C frontend \
    && mkdir backend/log \
    && mkdir backend/config \
    && mkdir frontend/log \
    && mkdir frontend/config

COPY dev_environments/standalone/standalone.sh .
COPY dev_environments/standalone/application.properties /app/backend/application.properties
COPY dev_environments/standalone/backend.conf /app/backend/config/server.conf
COPY dev_environments/standalone/frontend.conf /app/frontend/config/server.conf

ENTRYPOINT ["sh", "/app/standalone.sh"]
