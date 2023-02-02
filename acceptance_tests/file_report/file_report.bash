#!/bin/bash

if [ $# -lt 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env [timeout_in_minutes]" >&1
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

sh ../common/setup.sh

N_AGGREGATES=8

generate_input_file() {
    cd cstar-cli || exit
    echo "Generating input file..."
    poetry run cst sender aggregates --sender 12345 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
    cd ..
    FILENAME=$(basename cstar-cli/generated/*.csv)
    echo "Generated: $FILENAME"
    cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"
}
run_batch_service() {
    java -jar ../common/rtd-ms-transaction-filter.jar
}
get_file_sent_occurrence_in_report() {
    OUTPUT_FILENAME=$(basename workdir/output/ADE.*)
    FILE_REPORT_NAME=$(ls -v workdir/reports | tail -n 1)
    FILE_SENT_OCCURRANCE_IN_REPORT=$(grep -c "$OUTPUT_FILENAME" < workdir/reports/"$FILE_REPORT_NAME")
}

### file generation
generate_input_file

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

echo "Executing batch service..."
### batch service run
run_batch_service

#### ASSERTIONS

# the file sent is not in the report
get_file_sent_occurrence_in_report
if [ "$FILE_SENT_OCCURRANCE_IN_REPORT" != 0 ]
then
  echo "File sent has been found in report but it was not supposed to: [FAILED]"
  exit 2
fi

# check if file has been uploaded
N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" < workdir/logs/application.log)
if [ "$N_UPLOADS" -ne 1 ]
then
	echo "Upload test not passed, $N_UPLOADS files uploaded: [FAILED]"
	exit 2
else
	echo "Files uploaded with success: [SUCCESS]"
fi

SLEEP_INTERVAL_IN_SECONDS=10

#set batch service send to false in order to not send the placeholder files
export ACQ_BATCH_TRX_SENDER_ADE_ENABLED=false
for (( i=0 ; i <= TIMEOUT_IN_MINUTES * 60 / SLEEP_INTERVAL_IN_SECONDS; i++))
do
    echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
    sleep $SLEEP_INTERVAL_IN_SECONDS

    # run batch service with dummy file
    cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"
    run_batch_service

    # check if file sent in the previous run has been received inside the report
    get_file_sent_occurrence_in_report

    # if report does contain the file sent the exit loop
    if [ "$FILE_SENT_OCCURRANCE_IN_REPORT" -gt 0 ]
    then
        echo "file found in report: [SUCCESS]"
        break
    fi

done

exit 0
