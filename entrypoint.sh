#!/bin/sh

JKS_ENCODED="/tmp/certs.jks"
APP_ROOT="/app"
APP_BIN="$APP_ROOT/app.jar"
APP_CONFIG="$APP_ROOT/config.yml"
APP_WORKDIR="/app_workdir"
JKS_DECODED="$APP_ROOT/certs.jks"

# To remove in favor of configmap values
export HPAN_SERVICE_KEY_STORE_FILE="$APP_ROOT/certs.jks"
export HPAN_SERVICE_TRUST_STORE_FILE="$APP_ROOT/certs.jks"
export ACQ_BATCH_TOKEN_INPUT_PATH="$APP_WORKDIR/input"
export ACQ_BATCH_TRX_INPUT_PATH="$APP_WORKDIR/input"
export ACQ_BATCH_TRX_LOGS_PATH="$APP_WORKDIR/logs"
export ACQ_BATCH_OUTPUT_PATH="$APP_WORKDIR/output"
export ACQ_BATCH_HPAN_INPUT_PATH="$APP_WORKDIR/hpans"

mkdir -p $ACQ_BATCH_TOKEN_INPUT_PATH
mkdir -p $ACQ_BATCH_OUTPUT_PATH
mkdir -p $ACQ_BATCH_TRX_LOGS_PATH
mkdir -p $ACQ_BATCH_HPAN_INPUT_PATH
base64 -d $JKS_ENCODED > $JKS_DECODED

cd $APP_WORKDIR && java -jar $APP_BIN --spring.config.location=$APP_CONFIG
