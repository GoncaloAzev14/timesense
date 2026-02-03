#!/bin/sh

cd /app

npm install

if [ $NODE_ENV == "production" ]; then
    echo "Running in production mode"
    npm run start
else
    echo "Running in development mode"
    npm run dev
fi