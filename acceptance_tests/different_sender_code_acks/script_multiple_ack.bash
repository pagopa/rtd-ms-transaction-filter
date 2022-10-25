#!/bin/bash

if [ $# -lt 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env [timeout_in_minutes]" >&1
    exit 2
fi

if [ "$1" != "uat" ]
then
    echo "uat only available for this test!"
    exit 2
fi

ENV=$1
# test timeout default 2h (schedule pipeline + execution time pipeline)
TIMEOUT_IN_MINUTES="${2:-120}"

sh ../common/setup.sh

N_AGGREGATES=8

generate_input_file() {
    cd cstar-cli || exit
    echo "Generating input file..."
    poetry run cst sender aggregates --sender 12345 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
    poetry run cst sender aggregates --sender 99999 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
    cd ..
#    FILENAME=$(basename cstar-cli/generated/*.csv)
#    echo "Generated: $FILENAME"
    cp cstar-cli/generated/CSTAR.* workdir/input/
}
download_senderack_list() {
    ACKLIST_RESPONSE=$(curl \
                      --silent --output senderack_list.txt \
                      --write-out '%{http_code}' \
                      --cert ../certs/certificate.pem \
                      --key ../certs/private.key \
                      --header "Ocp-Apim-Subscription-Key: $HPAN_SERVICE_API_KEY" \
                      "$HPAN_SERVICE_URL/rtd/file-register/sender-ade-ack")
}
run_batch_service() {
    java -jar ../common/rtd-ms-transaction-filter.jar
}
get_number_acks_to_download() {
    ARRAY=$(grep -o '\[.*\]' < senderack_list.txt)
    if [ ${#ARRAY} == 2  ]
    then
        echo 0
    else
        NUM_COMMAS=$(grep -o ',' < senderack_list.txt | wc -l)
        if [ "$NUM_COMMAS" -eq 0 ]
        then
            echo 1
        else
            echo $(( NUM_COMMAS + 1 ))
        fi
    fi
}
check_sender_codes_from_list() {
    ARRAY=$(grep -o '\[.*\]' < senderack_list.txt)
    NUM_COMMAS=$(grep -o ',' <<< "$ARRAY" | wc -l)
    if [ "$NUM_COMMAS" -eq 0 ]
    then
        echo false
    else
        IS_99999_FOUND=$(grep -o "ACK.99999" <<< "$ARRAY" | wc -l)
        IS_12345_FOUND=$(grep -o "ACK.12345" <<< "$ARRAY" | wc -l)

        if [ "$IS_99999_FOUND" -eq 0 ] || [ "$IS_12345_FOUND" -eq 0 ]
        then
            echo false
        fi
    fi
    echo true
}
check_assertion() {
    N_FILES_TO_DOWNLOAD=$(get_number_acks_to_download)
    ARE_SENDER_CODES_FOUND=$(check_sender_codes_from_list)
    if [ "$N_FILES_TO_DOWNLOAD" -lt 2 ] || [ "$ARE_SENDER_CODES_FOUND" == false ]
    then
        echo false
    fi
    echo true
}
### file generation of 2 files with different senderCode
generate_input_file

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

echo "Executing batch service..."
### batch service run twice to process 2 input files
run_batch_service
run_batch_service

#### ASSERTIONS

# check if file has been uploaded
N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" < workdir/logs/application.log)
if [ "$N_UPLOADS" -ne 2 ]
then
	echo "Upload test not passed, $N_UPLOADS files uploaded: [FAILED]"
	exit 2
else
	echo "Files uploaded with success: [SUCCESS]"
fi

# find file in ade container
SLEEP_INTERVAL_IN_SECONDS=60
ADE_FILE_FOUND=true
for i in {1..10}
do
    echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
    sleep $SLEEP_INTERVAL_IN_SECONDS
    ADE_FILE_FOUND=true

    for output_file_path in workdir/output/ADE*.csv
    do
        # output filename e.g. ADE.12345.TRNLOG.20221019.120524.001.01.csv
        OUTPUT_FILENAME=$(basename "$output_file_path")
        if ! sh cstar-cli/integration_check/scripts/004-remote-file-check-UAT/script.sh workdir/output/"$OUTPUT_FILENAME" ../certs/certificate.pem ../certs/private.key "$HPAN_SERVICE_API_KEY"
        then
            #echo "file generated from $OUTPUT_FILENAME not found in ade/in"
            ADE_FILE_FOUND=false
            echo "Retrying..."
            break
        fi
    done
    if [ $ADE_FILE_FOUND == true ]
    then
        break
    fi
done

if [ $ADE_FILE_FOUND == false ]
then
    echo "File not found in ade container: [FAILED]"
    exit 2
fi

for output_file_path in workdir/output/ADE*.csv
do
    OUTPUT_FILENAME=$(basename "$output_file_path")
    # ade filename without extension e.g. AGGADE.12345.20221019.120524.001.01000
    ADE_FILENAME_WITHOUT_EXTENSION="AGGADE.$(echo "$OUTPUT_FILENAME" | cut -d'.' -f2,4-7)000"
    # inject file in ade/ack container
    if ! sh cstar-cli/integration_check/scripts/005a-extract-ade-ack-UAT/script.sh ./deposited-remotely/"$ADE_FILENAME_WITHOUT_EXTENSION" ../certs/certificate.pem ../certs/private.key "$HPAN_SERVICE_API_KEY"
    then
        echo "ack injection failed for $ADE_FILENAME_WITHOUT_EXTENSION: [FAILED]"
        exit 2
    fi
done

#set batch service send to false in order to not send the placeholder files
export ACQ_BATCH_TRX_SENDER_ADE_ENABLED=false
for (( i=0 ; i <= TIMEOUT_IN_MINUTES; i++))
do
    echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
    sleep $SLEEP_INTERVAL_IN_SECONDS

    # call senderack_list
    download_senderack_list

    if [ "$ACKLIST_RESPONSE" -ne 200 ]
    then
        exit 2
    fi

    IS_ASSERTION_SATISFIED=$(check_assertion)

    # if list do not contains at least 2 files with the expected sender codes, sleep
    if [ "$IS_ASSERTION_SATISFIED" == true ]
    then
        # run batch service to clear the acks available
        generate_input_file
        run_batch_service
        echo "Test [SUCCESS]"
        exit 0
    fi
done

echo "Found less than 2 files or not with the expected sender codes."
exit 2
