#!/bin/bash

echo "Starting PostgreSQL..."
su postgres -c "pg_ctl -D /var/lib/postgresql/data -l /var/lib/postgresql/logfile start"

echo "Starting the services..."
nginx
/app/backend/server.sh -f start
/app/frontend/server.sh -f start

tail -f frontend/log/* backend/log/*

