#!/bin/bash

if [ $# -lt 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env [timeout_in_minutes] [sender_code_1] [sender_code_2]" >&1
    exit 2
fi

if [ "$1" != "uat" ] &&  [ "$1" != "dev" ]
then
    echo "only dev and uat available for this test!"
    exit 2
fi


ENV=$1
# timeout default 5minutes
TIMEOUT_IN_MINUTES="${2:-5}"

SENDER_CODE1="${3-12345}"
SENDER_CODE2="${4-54321}"

sh ../common/setup.sh

N_AGGREGATES=8

REPORT_DIR=workdir/reports

echo "Make sure to have $SENDER_CODE1 and $SENDER_CODE2 associated with your API key"
echo "Make sure to have empty cstar-cli/generated folder"

generate_input_file() {
    cd cstar-cli || exit
    echo "Generating input file..."
    poetry run cst sender aggregates --sender $SENDER_CODE1 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
    poetry run cst sender aggregates --sender $SENDER_CODE2 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
    cd ..
    FILENAME=$(ls -1 cstar-cli/generated/*.csv | xargs -I {} basename {})
    echo "Generated:"
    echo "$FILENAME"
    cp cstar-cli/generated/*.csv ./workdir/input/
}
run_batch_service() {
    java -jar ../common/rtd-ms-transaction-filter.jar
}
get_file_sent_occurrence_in_report() {
    OUTPUT_FILENAME=$(ls workdir/output | grep -E "ADE.*.pgp")
    FILE_REPORT_NAME=$(ls -v "$REPORT_DIR" | tail -n 1)
    FILES_SENT_OCCURRENCES_IN_REPORT=$(grep -c "$OUTPUT_FILENAME" < "$REPORT_DIR"/"$FILE_REPORT_NAME")
}
check_two_files_sent() {
  # check if file has been uploaded
  N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" < workdir/logs/application.log)
  if [ "$N_UPLOADS" -ne 2 ]
  then
    echo "Upload test not passed, $N_UPLOADS files uploaded: [FAILED]"
    exit 2
  else
    echo "Files uploaded with success: [SUCCESS]"
  fi
}

### file generation
generate_input_file

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

export ACQ_BATCH_SCHEDULED=false
export ACQ_BATCH_FILE_REPORT_PATH="$REPORT_DIR"

echo "Executing batch service..."
### batch service run
run_batch_service
run_batch_service
#### ASSERTIONS

check_two_file_sent

SLEEP_INTERVAL_IN_SECONDS=10

#set batch service send to false in order to not send the placeholder files
export ACQ_BATCH_TRX_SENDER_ADE_ENABLED=false
for (( i=0 ; i <= TIMEOUT_IN_MINUTES * 60 / SLEEP_INTERVAL_IN_SECONDS; i++))
do
    echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
    sleep $SLEEP_INTERVAL_IN_SECONDS

    # run batch service with dummy file
    cp cstar-cli/generated/*.csv ./workdir/input/
    run_batch_service

    # check if file sent in the previous run has been received inside the report
    get_file_sent_occurrence_in_report

    # if report does contain the file sent the exit loop
    if [ "$FILES_SENT_OCCURRENCES_IN_REPORT" -gt 0 ]
    then
        echo "Files found in report: [SUCCESS]"
        break
    fi

done

rm -rf cstar-cli
rm -rf workdir

exit 0
