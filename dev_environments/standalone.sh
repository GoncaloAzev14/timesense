#!/bin/bash

echo "Starting the services..."
nginx start &
backend/server.sh -f start &
frontend/server.sh -f start &

tail -f frontend/log/* backend/log/*

