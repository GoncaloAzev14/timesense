FROM node:18-alpine

#RUN apk add --no-cache ca-certificates
COPY datacentric-tech.crt /usr/local/share/ca-certificates/datacentric-tech.crt
#RUN update-ca-certificates

ARG NPM_USER
ARG NPM_PASSWORD
ARG NPM_AUTH_TOKEN

COPY entrypoint.sh .

WORKDIR /app
# COPY package.json package-lock.json ./
COPY package.json ./

ENV NPM_USER ${NPM_USER}
ENV NPM_PASSWORD ${NPM_PASSWORD}
ENV NPM_AUTH_TOKEN ${NPM_AUTH_TOKEN}
ENV NODE_EXTRA_CA_CERTS /usr/local/share/ca-certificates/datacentric-tech.crt

RUN echo "@datacentric:registry=https://npm.datacentric-tech.com/" > ~/.npmrc \
  && echo "//npm.datacentric-tech.com/:_authToken=${NPM_AUTH_TOKEN}" >> ~/.npmrc \
  && echo "//npm.datacentric-tech.com/:email=npm@example.com" >> ~/.npmrc \
  && cat ~/.npmrc

#  && echo "//npm.datacentric-tech.com/:username=${NPM_USER}" >> ~/.npmrc \
#  && echo "//npm.datacentric-tech.com/:_password=${NPM_PASSWORD}" >> ~/.npmrc \

RUN npm install

# COPY . .
#
# RUN npm run build
# COPY .next ./.next

EXPOSE 3000

# wacth
