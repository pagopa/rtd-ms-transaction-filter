#!/bin/bash

if [ $# -ne 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env" >&1
    exit 2
fi

ENV=$1

sh ../common/setup.sh

N_AGGREGATES=12

### file generation
cd cstar-cli || exit
echo "Generating input file..."
# the sender code 11111 is not associated
poetry run cst sender aggregates --sender 11111 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
cd ..
FILENAME=$(basename cstar-cli/generated/*.csv)
cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

echo "Executing batch service..."
### batch service run
java -jar ../common/rtd-ms-transaction-filter.jar

#### ASSERTIONS
# 1. check local output files generated
# check output files generated
FILENAME_CHUNKED="ADE.$(echo "$FILENAME" | cut -d'.' -f2-6).01.csv"
if test -f "$ACQ_BATCH_OUTPUT_PATH/$FILENAME_CHUNKED"; then
    echo "$FILENAME_CHUNKED exists: [SUCCESS]"
else
    echo "$FILENAME_CHUNKED does not exist: [FAILED]"
    exit 2
fi

# 2. check application log to find evidence of failed files upload
N_UPLOADS_FAILED=$(grep -c "(status was: 401)" < workdir/logs/application.log)
if [ "$N_UPLOADS_FAILED" -ne 0 ]
then
    echo "File not uploaded: [SUCCESS]"
else
    echo "File has been uploaded: [FAILED]"
	  exit 2
fi

rm -rf cstar-cli
rm -rf workdir

exit 0
