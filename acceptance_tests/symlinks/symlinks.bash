#!/bin/bash

if [ $# -lt 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash symlinks.bash env" >&1
    exit 2
fi

if [ "$1" != "uat" ] &&  [ "$1" != "dev" ]
then
    echo "only dev and uat available for this test!"
    exit 2
fi

ENV=$1

sh ../common/setup.sh
ln -s workdir/input symlink-input

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
if [ "$N_UPLOADS" -eq 1 ]
then
  echo "Files uploaded with success: [SUCCESS]"
else
  echo "Upload test not passed, $N_UPLOADS files uploaded: [FAILED]"
	exit 2
fi

#rm -rf cstar-cli
#rm -rf workdir

exit 0
