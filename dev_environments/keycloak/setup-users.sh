#!/bin/sh

main() {
    access_token=$(login_keycloak)
    echo $access_token
    createUser $access_token DataSync adm adm John Administrator
    createUser $access_token DataSync mngr mngr Sarah Manager
    createUser $access_token DataSync user user Tom User
}

login_keycloak() {
    access_token=`curl -s -H "Content-Type: application/x-www-form-urlencoded" \
                    -d "client_id=admin-cli" -d "username=${KEYCLOAK_ADMIN}" \
                    -d "password=${KEYCLOAK_ADMIN_PASSWORD}" -d "grant_type=password" \
                    -d "scope=openid" "http://keycloak:8080/keycloak/realms/master/protocol/openid-connect/token" | \
                sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'`
    echo $access_token
}

createUser() {
    access_token=$1
    realm=$2
    userName=$3
    userPassword=$4
    userFirstName=$5
    userLastName=$6


    echo "Using url http://keycloak:8080/keycloak/admin/realms/${realm}/users"
    curl -X POST http://keycloak:8080/keycloak/admin/realms/${realm}/users -s \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${access_token}" \
        -d "{
        \"username\": \"${userName}\",
        \"email\": \"${userName}@home\",
        \"firstName\": \"${userFirstName}\",
        \"lastName\": \"${userLastName}\",
        \"enabled\": true,
        \"credentials\": [
          {
            \"type\": \"password\",
            \"value\": \"${userPassword}\",
            \"temporary\": false
          }
        ]
    }"
    echo "Creating user ${userName}: $?"
}

main
