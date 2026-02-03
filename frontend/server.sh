#!/bin/sh

SCRIPT_DIR=`dirname "$0"`
LOG_DIR="${SCRIPT_DIR}/log"
PID_FILE="${LOG_DIR}/.pid"
NODE_CMD="/usr/bin/node"
LOG_OUTPUT=${SERVER_LOG_OUTPUT:-console}
SERVER_OUTPUT_NAME="server.`date +%Y%m%d%H%M%S`"

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

if [ -f ${SCRIPT_DIR}/config/server.conf ]; then
    . ${SCRIPT_DIR}/config/server.conf
fi

start() {
    if is_process_running; then
        echo "Server is already running. PID: $(cat ${PID_FILE})"
        exit 1
    fi
    cd ${SCRIPT_DIR}
    if [ "${LOG_OUTPUT}" = "console" ]; then
        ${NODE_CMD} server.js &
        PID=$!
        echo $PID > ${PID_FILE}
        wait $PID
    else
        ${NODE_CMD} server.js \
            > ${LOG_DIR}/${SERVER_OUTPUT_NAME}.out \
            2> ${LOG_DIR}/${SERVER_OUTPUT_NAME}.err &
        echo $! > ${PID_FILE}
    fi
}

stop() {
    if [ ! -f ${PID_FILE} ]; then
        echo "Server is not running."
        exit 1
    fi
    kill $(cat ${PID_FILE})
    rm ${PID_FILE}
}

is_process_running() {
    if [ ! -f ${PID_FILE} ]; then
        return 1
    fi
    ps -p $(cat $PID_FILE) q >/dev/null
}

status() {
    if is_process_running; then
        echo "Server is not running."
    fi
}

help() {
    echo "Usage: $0 [OPTIONS] COMMAND"
    echo ""
    echo "Options:"
    echo "  -h, --help        Show this help message and exit"
    echo "  -f, --file        Log output to a file"
    echo "  -c, --console     Log output to the console"
    echo "  --log-path PATH   Specify the log file path (requires --file)"
    echo ""
    echo "Commands:"
    echo "  start             Start the service"
    echo "  stop              Stop the service"
    echo "  restart           Restart the service"
    echo "  status            Show service status"
    echo "  help              Show this help message and exit"
    exit 0
}

main(){
    # Parse options
while [ "$#" -gt 0 ]; do
    case "$1" in
        -h|--help)
            help
            ;;
        -f|--file)
            LOG_OUTPUT="file"
            ;;
        -c|--console)
            LOG_OUTPUT="console"
            ;;
        --log-path)
            if [ -n "$2" ] && [ "${2#-}" = "$2" ]; then
                USER_LOG_DIR="$2"
                shift
            else
                echo "Error: --log-path requires a valid path argument." >&2
                exit 1
            fi
            ;;
        start|stop|restart|status|help)
            COMMAND="$1"
            ;;
        *)
            echo "Unknown option or command: $1" >&2
            exit 1
            ;;
    esac
    shift
done
# Validate log-path with file option
if [ -n "$USER_LOG_DIR" ] && [ "$LOG_OUTPUT" != "file" ]; then
    echo "Error: When --log-path is defined, the --file option must be set." >&2
    exit 1
elif [ -n "$USER_LOG_DIR" ]; then
    LOG_DIR="$USER_LOG_DIR"
fi


# Execute command
case "$COMMAND" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    status)
        status
        ;;
    help)
        help
        ;;
    *)
        echo "Unknown command. Use one of start, stop, restart, status, or help." >&2
        exit 1
        ;;
esac
}

main "$@"
