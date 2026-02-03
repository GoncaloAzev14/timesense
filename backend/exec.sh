#!/bin/sh
SCRIPT_DIR=$(realpath $(dirname $0))
SCRIPT_FILE=${SCRIPT_DIR}/server.sh

log(){
    log_message=$1
    level=$([ -n "$2" ] && echo "$2" || echo "INFO")
    COLOR="\033[36;1m"
    NON_COLOR="\033[0;37m"
    [ ${level} == "ERROR" ] && COLOR="\033[0;31m";
    [ ${level} == "WARNING" ] && COLOR="\033[0;33m";

    printf "`date +"%Y-%m-%d %H:%M:%S"` ${COLOR}${level}${NON_COLOR} ${log_message}\n"
}

restart_application(){

    log "Restart Application";

    $SCRIPT_FILE restart &

    log "Application restarted!";
}

loop(){

    while true; do
        CURRENT_MOD_TIME=$([ -f "$JAR_FILE" ] && stat -c %Y "$JAR_FILE")

        if [ -n "$CURRENT_MOD_TIME" ] && [ "$CURRENT_MOD_TIME" -ne "$LAST_MOD_TIME" ]; then
            log "Changes have been detected!";
            LAST_MOD_TIME=$CURRENT_MOD_TIME
            restart_application
        fi

        sleep $INTERVAL;
    done
}


if [ "${JAR_FILE}" == "" ]; then
    JAR_FILE=`ls /app/lib/timesense-backend-*.jar|head -n 1`
fi
INTERVAL=5s
LAST_MOD_TIME=$(stat -c %Y "$JAR_FILE")

cd /app
$SCRIPT_FILE start &
loop
