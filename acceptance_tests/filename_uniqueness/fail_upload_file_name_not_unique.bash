#!/bin/bash

if [ $# -ne 1 ]; then
  echo "Illegal number of parameters (1 mandatory, was $#)" >&1
  echo "usage: bash script_splitting.bash env" >&1
  exit 2
fi

ENV=$1

N_AGGREGATES=10

RETRY_MAX_ATTEMPTS=1

sh ../common/setup.sh

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
  echo "Executing batch service..."
  java -jar ../common/rtd-ms-transaction-filter.jar
}
check_one_file_sent() {
  # check if the first file has been uploaded
  N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" <workdir/logs/application.log)
  if [ "$N_UPLOADS" -ne 1 ]; then
    echo -e "Upload test not passed, $N_UPLOADS files uploaded: \033[1m\033[31m[FAILED]\033[0m"
    exit 2
  else
    echo -e "File uploaded with success: \033[1m\033[32m[SUCCESS]\033[0m"
  fi
}
check_one_file_not_uploaded_due_to_conflict() {
  # check if the second file is not uploaded because has the same name of the first
  N_UPLOADS=$(grep -c "(status was 409:" <workdir/logs/application.log)
  if [ "$N_UPLOADS" -ne "$RETRY_MAX_ATTEMPTS" ]; then
    echo -e "Test not passed, 409 for the second file not returned: \033[1m\033[31m[FAILED]\033[0m"
    exit 2
  else
    echo -e "Test passed, 409 for the second file returned: \033[1m\033[32m[SUCCESS]\033[0m"
  fi
}

### file generation
generate_input_file

### batch service configuration
source ../common/setenv_"$ENV".sh
source setenv.sh

### batch service run
run_batch_service

# Assertion 1. check one file uploaded correctly
check_one_file_sent

# Second Batch Service run with the same file
cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"

run_batch_service

# Assertion 2. check application log to find evidence of failed files upload
check_one_file_not_uploaded_due_to_conflict

rm -rf ./cstar-cli
rm -rf ./workdir

exit 0
