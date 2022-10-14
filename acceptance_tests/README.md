# Acceptance tests

## Directory layout
```
acceptance_tests
    ├── certs       <-- jks, mauth cert and key
    ├── common       <-- common files (jar, setup)
    └── tests        <-- every test in a dedicated folder
```


## Usage

1. Download the batch service executable file:
    ```
    wget https://github.com/pagopa/rtd-ms-transaction-filter/releases/latest/download/rtd-ms-transaction-filter.jar
    ```
2. Place the `rtd-ms-transaction-filter.jar` file in the `acceptance_tests/common` directory
2. Create the `certs.jks` and place it in the `certs` folder (instruction [here](https://docs.pagopa.it/centrostella-1/centro-stella/instructions-for-agenzia-delle-entrate-mandate/how-to-join/03.-configure-the-batch-service/prepare-the-java-keystore))
3. Put the private key and the corresponding certificate for mutual-auth in the folder `certs`
4. Create a copy of the file `acceptance_tests/common/setenv_sample` and rename it as `setenv_dev.sh` 
| `setenv_uat.sh`
5. Customize env variables in `setenv_dev.sh` | `setenv_uat.sh`
6. Run a test with `bash script_splitting.bash dev`


