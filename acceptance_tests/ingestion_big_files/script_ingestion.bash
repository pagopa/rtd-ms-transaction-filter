#!/bin/bash

# This script tests the ingestion of a file of big dimension.
# By default it will generate a single chunk of 6M rows but it is
# customizable to generate more chunks by tuning N_AGGREGATES and
# ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD values

if [ $# -ne 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env [timeout_in_minutes]" >&1
    exit 2
fi

ENV=$1
# timeout default 250m (aggregates-ingestor takes around 3h to complete)
TIMEOUT_IN_MINUTES="${2:-250}"

sh ../common/setup.sh

N_AGGREGATES=6000000

### file generation
cd cstar-cli || exit
echo "Generating input file..."
poetry run cst sender aggregates --sender 12345 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 2
cd ..
FILENAME=$(basename cstar-cli/generated/*.csv)
cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

# check env script
# if [ $ACQ_BATCH_INPUT_CHUNK_SIZE -gt $ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD ]
# then
# 	echo "Please set the chunk size and ade threshold appropriately"
# 	exit 2
# fi

echo "Executing batch service..."
### batch service run
java -jar ../common/rtd-ms-transaction-filter.jar

#### ASSERTIONS
# 1. check local output files generated
# set threshold if not present
if [ -z "$ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD" ]
then
    ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD=1000000
fi

# find chunks number
if [ $N_AGGREGATES -lt $ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD ]
then
	  N_CHUNKS=1
else
	  N_CHUNKS=$(( ( N_AGGREGATES / ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD ) + ( N_AGGREGATES % ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD > 0 ) ))
fi

OUTPUT_WITHOUT_EXTENSION="ADE.$(echo "$FILENAME" | cut -d'.' -f2-6)"
# check output files generated
for (( i=1; i<=N_CHUNKS; i++ ))
do
    # there is a limitation to 9 chunks!!!!
    FILENAME_CHUNKED="$OUTPUT_WITHOUT_EXTENSION.0$i.csv"
    if test -f "$ACQ_BATCH_OUTPUT_PATH/$FILENAME_CHUNKED"; then
        echo "$FILENAME_CHUNKED exists: [SUCCESS]"
    else
        echo "$FILENAME_CHUNKED does not exist: [FAILED]"
        exit 2
    fi
done

# 2. check application log to find evidence of successful files upload
N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" < workdir/logs/application.log)
if [ $N_CHUNKS -ne "$N_UPLOADS" ]
then
    echo "Upload test not passed, $N_CHUNKS chunks vs $N_UPLOADS uploaded: [FAILED]"
    exit 2
else
	  echo "Files uploaded with success: [SUCCESS]"
fi

# cli atm do not support chunk generation, it can be arranged like this:
LOCAL_OUTPUT_DIFF=$(diff <(cat cstar-cli/generated/ADE.*expected | sort) <(cat workdir/output/ADE.*.csv | sort | tail -n +$N_CHUNKS))

if [ -z "$LOCAL_OUTPUT_DIFF" ]
then
  	echo "Diff local output with expected: [SUCCESS]"
else
    echo "Diff local output with expected: [FAILED]"
    exit 2
fi

# 3. check files on ade container

# timeout should be parametrized on the dimension of the input file
SLEEP_INTERVAL_IN_SECONDS=60
ADE_FILENAME_WITHOUT_EXTENSION=AGGADE.$(echo "$OUTPUT_WITHOUT_EXTENSION"  | cut -d'.' -f2,4-6)
for (( i=0 ; i <= TIMEOUT_IN_MINUTES; i++))
do
    echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
    sleep $SLEEP_INTERVAL_IN_SECONDS

    ALL_FILES_FOUND=true
    for (( j=0; j<3; j++ ))
    do
        ADE_FILENAME_CHUNKED="$ADE_FILENAME_WITHOUT_EXTENSION.0100${j}.gz"
        RESPONSE=$(curl \
                  --silent --output "$ADE_FILENAME_CHUNKED" \
                  --write-out '%{http_code}' \
                  --cert ../certs/certificate.pem \
                  --key ../certs/private.key \
                  --header "Ocp-Apim-Subscription-Key: $HPAN_SERVICE_API_KEY" \
                  "$HPAN_SERVICE_URL/rtd/sftp-retrieve/$ADE_FILENAME_CHUNKED")

        if [ "$RESPONSE" -eq 404 ]
        then
            echo "file $ADE_FILENAME_CHUNKED not found"
            rm "$ADE_FILENAME_CHUNKED"
            ALL_FILES_FOUND=false
            echo "Retrying..."
            break
        fi
    done

    if [ $ALL_FILES_FOUND == true ]
    then
        echo "All Ade files found: [SUCCESS]"
        break
    fi
done

if [ $ALL_FILES_FOUND == false ]
then
    echo "Ade files not found: [FAILED]"
	  exit 2
fi

exit 0


