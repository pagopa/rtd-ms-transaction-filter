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
# timeout default 1h (schedule pipeline + execution time pipeline)
TIMEOUT_IN_MINUTES="${2:-90}"

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

# check if file has been uploaded
N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" < workdir/logs/application.log)
if [ "$N_UPLOADS" -ne 1 ]
then
	echo "Upload test not passed, $N_UPLOADS files uploaded: [FAILED]"
	exit 2
else
	echo "Files uploaded with success: [SUCCESS]"
fi

# find file in ade container
SLEEP_INTERVAL_IN_SECONDS=60
# output without extension e.g. ADE.12345.TRNLOG.20221019.120524.001.01
OUTPUT_FILENAME=$(basename workdir/output/ADE.*)
# ade filename without extension e.g. AGGADE.12345.20221019.120524.001.01000
ADE_FILENAME_WITHOUT_EXTENSION="AGGADE.$(echo "$OUTPUT_FILENAME" | cut -d'.' -f2,4-7)000"
ADE_FILE_FOUND=true
for i in {1..10}
do
    echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
    sleep $SLEEP_INTERVAL_IN_SECONDS
    ADE_FILE_FOUND=true

    if ! sh cstar-cli/integration_check/scripts/004-remote-file-check-UAT/script.sh workdir/output/"$OUTPUT_FILENAME" ../certs/certificate.pem ../certs/private.key "$HPAN_SERVICE_API_KEY"
    then
        echo "file $ADE_FILENAME_WITHOUT_EXTENSION not found in ade/in"
        ADE_FILE_FOUND=false
        echo "Retrying..."
        continue
    else
        break
    fi
done

if [ $ADE_FILE_FOUND == false ]
then
    echo "File not found in ade container: [FAILED]"
    exit 2
fi

# inject file in ade/ack container
if ! sh cstar-cli/integration_check/scripts/005a-extract-ade-ack-UAT/script.sh ./deposited-remotely/"$ADE_FILENAME_WITHOUT_EXTENSION" ../certs/certificate.pem ../certs/private.key "$HPAN_SERVICE_API_KEY"
then
    echo "test failed"
    exit 2
fi

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

    N_FILES_TO_DOWNLOAD=$(get_number_acks_to_download)
    echo "$N_FILES_TO_DOWNLOAD files to download"

    # if list do not contains files, sleep
    if [ "$N_FILES_TO_DOWNLOAD" -eq 0 ]
    then
        continue
    fi
    # otherwise run batch service
    generate_input_file
    run_batch_service
    # recall senderack_list
    download_senderack_list
    # numbers of files to download must be lesser than before
    N_FILES_TO_DOWNLOAD_POST_BATCH=$(get_number_acks_to_download)

    if [ "$N_FILES_TO_DOWNLOAD_POST_BATCH" -lt "$N_FILES_TO_DOWNLOAD" ]
    then
        echo "TEST [SUCCESS]"
        break
    else
        echo "TEST [FAILED]"
        exit 2
    fi
done

exit 0
