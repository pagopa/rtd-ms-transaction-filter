#!/bin/bash

if [ $# -ne 1 ] ; then
    echo "Illegal number of parameters (1 mandatory, was $#)" >&1
    echo "usage: bash script_splitting.bash env" >&1
    exit 2
fi

ENV=$1

sh ../common/setup.sh

N_AGGREGATES=18

### batch service configuration
# shellcheck source=../common/setenv_env.sh
source ../common/setenv_"$ENV".sh
source setenv.sh

#retrieve public key to encrypt files
RESPONSE=$(curl \
      --silent --output public.key \
      --write-out '%{http_code}' \
      --cert ../certs/certificate.pem \
      --key ../certs/private.key \
      --header "Ocp-Apim-Subscription-Key: $HPAN_SERVICE_API_KEY" \
      "$HPAN_SERVICE_URL/rtd/csv-transaction/publickey")

if [ "$RESPONSE" -ne "200" ]
then
	echo "Error in retrieving public key!"
	exit 2
fi

### file generation
cd cstar-cli || exit
echo "Generating input file..."
# file placeholder, Ade sender is disabled by batch service configuration
poetry run cst sender aggregates --sender 12345 --action trx_and_aggr --aggr-qty $N_AGGREGATES --avg-trx 3
# file to send
poetry run cst tae transactionaggregate --action aggregate_transactions --sender 12345 --aggr-qty $N_AGGREGATES --reverse-ratio 100 --ratio-dirty-pos-type 50 --ratio-dirty-vat 50 --shuffle --out-dir ../workdir/output/pending --pgp --key ../public.key
poetry run cst tae transactionaggregate --action aggregate_transactions --sender 12345 --aggr-qty $N_AGGREGATES --reverse-ratio 100 --ratio-dirty-pos-type 50 --ratio-dirty-vat 50 --shuffle --out-dir ../workdir/output/pending --pgp --key ../public.key
cd ..
FILENAME=$(basename cstar-cli/generated/*.csv)
cp cstar-cli/generated/"$FILENAME" ./workdir/input/"$FILENAME"

echo "Executing batch service..."
### batch service run
java -jar ../common/rtd-ms-transaction-filter.jar

#### ASSERTIONS
# 1. check application log to find evidence of successful files upload
N_UPLOADS=$(grep -c "uploaded with success (status was: 201)" < workdir/logs/application.log)
if [ "$N_UPLOADS" -ne 2 ]
then
	echo "Upload test not passed, $N_UPLOADS files uploaded: [FAILED]"
	exit 2
else
	echo "Files uploaded with success: [SUCCESS]"
fi

exit 0


