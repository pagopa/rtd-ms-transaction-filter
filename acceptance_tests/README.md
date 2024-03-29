# Acceptance tests

## Directory layout
```
acceptance_tests
    ├── certs       <-- jks, mauth cert and key
    ├── common       <-- common files (jar, setup)
    └── tests        <-- every test in a dedicated folder
```

## Test environment configuration
The precondition for the tests is to have the sender-auth properly configured with the association between the sender codes and sender api key.

## Usage

1. Download the batch service executable file:
    ```
    wget https://github.com/pagopa/rtd-ms-transaction-filter/releases/latest/download/rtd-ms-transaction-filter.jar
    ```
2. Put the `rtd-ms-transaction-filter.jar` file in the `common` directory
3. Create the `certs.jks` and place it in the `certs` folder (instruction [here](https://docs.pagopa.it/centrostella-1/centro-stella/instructions-for-agenzia-delle-entrate-mandate/how-to-join/03.-configure-the-batch-service/prepare-the-java-keystore))
4. Put the private key and the corresponding certificate for mutual-auth in the folder `certs`, named respectively `private.key` and `certificate.pem`
5. Create a copy of the file `common/setenv_sample` and rename it as `setenv_dev.sh` 
| `setenv_uat.sh`
6. Customize env variables in `setenv_dev.sh` | `setenv_uat.sh`
7. Run a test with `bash script_splitting.bash dev`


