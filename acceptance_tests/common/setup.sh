### create directories
mkdir -p workdir/input
mkdir -p workdir/hpans
mkdir -p workdir/ade-errors
mkdir -p workdir/output/pending
mkdir -p workdir/logs
mkdir -p workdir/reports

git clone --depth 1 https://github.com/pagopa/cstar-cli.git
cd cstar-cli || exit
poetry install
poetry run ./install.sh
