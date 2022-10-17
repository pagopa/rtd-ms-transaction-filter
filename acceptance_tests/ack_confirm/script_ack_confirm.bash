#!/bin/bash

### create directories
mkdir -p workdir/input
mkdir -p workdir/hpans
mkdir -p workdir/ade-errors
mkdir -p workdir/output
mkdir -p workdir/logs

N_AGGREGATES=8

### file generation
git clone --depth 1 https://github.com/pagopa/cstar-cli.git
cd cstar-cli
poetry install
poetry run ./install.sh
poetry run cst sender aggregates --sender 12345 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
cd ..
FILENAME=$(basename cstar-cli/generated/*.csv)
cp cstar-cli/generated/$FILENAME ./workdir/input/$FILENAME

### batch service configuration
# git clone --depth 1 --branch develop https://github.com/pagopa/rtd-ms-transaction-filter.git
# cd rtd-ms-transaction-filter
# echo "Packaging batch service..."
# mvn clean package > /dev/null
# cd ..
# cp rtd-ms-transaction-filter/target/rtd-ms-transaction-filter.jar ./rtd-ms-transaction-filter.jar
source setenv.sh

# check env script
# if [ $ACQ_BATCH_INPUT_CHUNK_SIZE -gt $ACQ_BATCH_WRITER_ADE_SPLIT_THRESHOLD ]
# then
# 	echo "Please set the chunk size and ade threshold appropriately"
# 	exit 2
# fi

### batch service run
java -jar ../rtd-ms-transaction-filter.jar

#### ASSERTIONS


OUTPUT_WITHOUT_EXTENSION="ADE.$(echo $FILENAME | cut -d'.' -f2-6)"

#ciclo 10 volte
	# sleep 1 minuto
	# eseguo asserzione
	# true test finito
	# else false continuo il ciclo
# test fallito

# il timeout totale andrebbe parametrizzato in base alla dimensione del file di input
SLEEP_INTERVAL_IN_SECONDS=60
ADE_FILENAME_WITHOUT_EXTENSION=AGGADE.$(echo $OUTPUT_WITHOUT_EXTENSION  | cut -d'.' -f2-6)
for i in {1..10}
do
	echo "Waiting $SLEEP_INTERVAL_IN_SECONDS seconds..."
	sleep $SLEEP_INTERVAL_IN_SECONDS

done

exit 0


