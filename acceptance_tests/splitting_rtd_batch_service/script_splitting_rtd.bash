#!/bin/bash

if [ $# -ne 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env" >&1
    exit 2
fi

ENV=$1

sh ../common/setup.sh

N_TRANSACTIONS=500

### file generation
cd cstar-cli || exit
echo "Generating input file..."
poetry run cst rtd transactionfilter --action synthetic_hashpans --pans-prefix "pan" --hashpans-qty 200 --salt FAKE_SALT > 200_hpans.csv
poetry run cst rtd transactionfilter --action synthetic_transactions --sender 12345 --pans-prefix "pan" --pans-qty 200 --trx-qty $N_TRANSACTIONS --ratio 1 --pos-number 100
cd ..
FILENAME=$(basename cstar-cli/generated/*.csv)
cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"
cp cstar-cli/200_hpans.csv ./workdir/hpans/

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

echo "Executing batch service..."
### batch service run
java -jar ../common/rtd-ms-transaction-filter.jar

#### ASSERTIONS
# 1. check local output files generated
# find chunks number
if [ $N_TRANSACTIONS -lt "$ACQ_BATCH_WRITER_RTD_SPLIT_THRESHOLD" ]
then
	N_CHUNKS=1
else
	N_CHUNKS=$(( ( N_TRANSACTIONS / ACQ_BATCH_WRITER_RTD_SPLIT_THRESHOLD ) + ( N_TRANSACTIONS % ACQ_BATCH_WRITER_RTD_SPLIT_THRESHOLD > 0 ) ))
fi

# check output files generated
for (( i=1; i<=N_CHUNKS; i++ ))
do
	# there is a limitation to 9 chunks!!!!
	FILENAME_CHUNKED="CSTAR.$(echo "$FILENAME" | cut -d'.' -f2-6).0$i.csv"
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
N_ROWS_OUTPUT=$(cat workdir/output/CSTAR.*.csv | wc -l)

if [ "$N_ROWS_OUTPUT" -eq $(( N_TRANSACTIONS + N_CHUNKS )) ]
then
  echo "Number of rows expected: [SUCCESS]"
else
	echo "Wrong number of rows: [FAILED]"
	exit 2
fi

exit 0
