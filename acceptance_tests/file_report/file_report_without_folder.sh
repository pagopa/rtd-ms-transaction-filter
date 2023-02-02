#!/bin/sh

if [ $# -lt 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env" >&1
    exit 2
fi

if [ "$1" != "uat" ] &&  [ "$1" != "dev" ]
then
    echo "only dev and uat available for this test!"
    exit 2
fi

ENV=$1

N_AGGREGATES=8

REPORT_DIR=workdir/reports

sh ../common/setup.sh

rm -rf "$REPORT_DIR"

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
     OUTPUT_FILENAME=$(ls workdir/output | grep -E "ADE.*.pgp")
     FILE_REPORT_NAME=$(ls -v "$REPORT_DIR" | tail -n 1)
     FILE_SENT_OCCURRENCE_IN_REPORT=$(grep -c $OUTPUT_FILENAME < "$REPORT_DIR"/"$FILE_REPORT_NAME")
 }

### file generation
generate_input_file

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

# force reports directory to be workdir/reports
export ACQ_BATCH_FILE_REPORT_PATH="$REPORT_DIR"
export ACQ_BATCH_TRX_SENDER_ADE_ENABLED=false

echo "Executing batch service..."
### batch service run
run_batch_service
#### ASSERTIONS

# expected result: job went fine and report directory has been created
if [ -d "$ACQ_BATCH_FILE_REPORT_PATH" ]
then
  echo "Directory "$REPORT_DIR" does exists: [SUCCESS]"
else
  echo "Directory "$REPORT_DIR" does not exists: [FAILED]"
  exit 2
fi

# one report is downloaded
if [ "$(ls -1 "$REPORT_DIR" | wc -l)" -eq 1 ]
then
  echo "Directory "$REPORT_DIR" is not empty: [SUCCESS]"
else
  echo "Directory "$REPORT_DIR" is empty: [FAILED]"
  exit 2
fi

rm -rf cstar-cli
rm -rf workdir

exit 0
