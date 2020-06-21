# Acquirer Transaction Filter Batch

Component defining the batch process for filtering the input transaction records, based on a list of pan, recovered from a local directory or through a remote service. 

### Execution requirements

The artifact consists of an executable jar produced with _spring-boot_, therefore all the project dependencies are contained within the jar, together with the classes that contains the business logic.
The artifact is completely autonomous and usable on any device that has a JVM

To install and run the batch, it's required:
- _Java 1.8+_
- _batch-transaction-filter.jar_ artifact

For the application of PGP encryption to the result file, produced by the batch, there will also need a file containing the public key to be used, reported in __Appendix 1 - PGP public key__.
For the application of decryption of the PGP pan file, it must provided a file containing the secret key to be applied for the operation.
To produce the artifact from the source code it will be necessary to have an installation of Maven and a java compiler (jdk1.8+).

### Bundle Distribution

The batch-transaction-filter.jar artifact will be provided in a bundle containing a folder with the configurations and the structures of the files, in order to allow an immediate execution with the default configuration, which point to path/folders listed in the bundle. The default service will poll to check for files to be processed every minute.

Inside the bundle there are also the public key file shown in the _appendix_, and some sample for a first test run.
 
__Nota:__ The bundle contains a version potentially out of alignment with the implementation. In the default configuration attempts to connect to the REST services and the sftp comunication are disabled.

The bundle structure and the files is contains are described below:

- _batch-transaction-filter.jar_, the artifact containing the batch service
- _/config_, folder containing the configuration files
- _/config/application.yml_, file containing the configuration properties for the service
- _/resources_, folder containing the resources and folder for running the bundle under a default configuration
- _/hpans_, folder where to insert the files containing the pan list
- _/keys_, folder containing the keys for pgp encryption
- _/transactions_, folder where to insert the files containing the transactions to be processed
- _/output_, folder where the files produced by the service will be inserted
- _/sample-files_, folder containing test files for execution

### Generating artifact from source

To obtain a version of the artifact produced directly from the source code, a Maven instance must be appropriately configured on the
machine to use via the command line. Once the source has been downloaded, run the following command from the root directory:

> mvn clean package __<execution_options>__
	
If the command is executed without other options, the artifact will be produced once the unit tests (declared in the module) have been
performed. To perform the operation without waiting for the execution and validation of the tests, run the command in the following
form:

>mvn clean package -DskipTests
	
The artifact will be created into the target folder at root level

### Database connection

Spring Batch uses a repository on which you can track the executions performed by the service. 
If there is no particular configuration: an in- memory instance will be executed to allow the batch to be executed. The configuration of
the bundle uses this mode for greater immediacy of use. If you want to set it please refer to the properties in Appendix 2 -
Configuration properties.

### Execution guidelines

- Install and configure the environment so that the Java 1.8+ version is available, as indicated in the prerequisites         

- In case of execution of bundled version, extract artifact and resource in a position of your choice, if no additional   
  configuration is required, refer to the execution step at the end of the paragraph. 
  Consider whether to use the sample files contained in the _transactions_ and _hpan_ folders.         

- If you are not using the bundled version, please produce a version of the artifact via source code, as indicated in the corresponding
  paragraph of the manual. Prepare a configuration application.yml file and, if needed, other files .yml or .properties to be used for
  the configuration properties.

- Place the _batch-transaction-filter.jar_ artifact in a location of your choice

- Place in a location of your choice, the configuration files, supplied together with the artifact in the bundle, or
  your own.

- Place on the machine, the files of the public and/or private key for pgp, if one of the file encryption/decryption function is active. 

- Configure the path to the file containing the public key, through the
  batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath property, or through the environment variable
  _ACQ_BATCH_INPUT_PUBLIC_KEYPATH_.         
   
   __Nota:__ The configuration is strictly needed only if the encryption function of the produced files is enabled. In the case of
   configuration on file, the path must be preceded by the prefix _file:/_. for example:
   
    >batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath = file:/C/:Development/keys/public.asc

- Configure the pointing to the file containing the private key, through the property
  _batchConfiguration.TransactionFilterBatch.panList.secretKeyPath_, or through the environment variable
  _ACQ_BATCH_INPUT_SECRET_KEYPATH_. 

  __Nota:__ The configuration is strictly necessary only if the decryption function of the files containing the pan list is enabled. 
  In the case of configuration on file, the path must be preceded by prefix _file:/_. for example::

    >batchConfiguration.TransactionFilterBatch.panList.secretKeyPath = file:/C:/Development/keys/secret.asc
	
- Configure the passphrase to be applied if the secret key is enabled, through the
  _batchConfiguration.TransactionFilterBatch.panList.passphrase_ property , or via the _ACQ_BATCH_INPUT_SECRET_PASSPHRASE_ environment
  variable.         

- Define a folder where the path files, to be processed, will be placed

- Configure the path to the transaction files to be processed, through the
  _batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath_ property, or through the environment variables
  _ACQ_BATCH_TRX_INPUT_PATH_ for the folder, and _ACQ_BATCH_INPUT_FILE_PATTERN_, for the pattern of files to read. 

  __Nota:__  In the case of file configuration, the path must be preceded by the prefix _file:/_. for example: 

  >batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath = file:/C:/Development/transactions/*.csv

- Define a folder for the files containing the PAN list 

- Configure the path to the files containing the pan list, through the
 _batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath_ property , or through the environment variables
 _ACQ_BATCH_HPAN_INPUT_PATH_ for the folder, and  _ACQ_BATCH_HPAN_INPUT_FILE_PATTERN_, for the pattern of files to read.
 
  __Nota:__  In the case of configuration on file, the path must be preceded by the prefix _file:/_. for example: 

  >batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath = file:/C:/Development/hpan/*.pgp

- Define a folder for the output files

- Configure the pointing to the trace files to be processed, through the property
  _batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath_, or through the environment variable
  _ACQ_BATCH_OUTPUT_PATH_      

  __Nota:__  In the case of configuration on file, the path must be preceded by the prefix _file:/_. for example:

  >batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath = file:/C:/Development/output

- Configure the hashing application for the pan list, through the _batchConfiguration.TransactionFilterBatch.panList.applyHashing_
  property, or through the environment variable _ACQ_BATCH_PAN_LIST_APPLY_HASHING_        

- Configure for decryption of the file containing the pan list, through the
  _batchConfiguration.TransactionFilterBatch.panList.applyDecrypt_ property, or through the environment variable
  _ACQ_BATCH_PAN_LIST_APPLY_DECRYPT_         

- Configure the hash application for transactions, through the batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing
  property, or through the environment variable ACQ_BATCH_TRX_LIST_APPLY_HASHING

- Configure for product encryption, through the batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt property, or
  through the environment variable ACQ_BATCH_TRX_LIST_APPLY_ENCRYPT

- Configure for the hash application in the transactions reported in the product file, through the
  _batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing_ property, or through the environment variable
  _ACQ_BATCH_TRX_LIST_HASHING_SAVE_         

- Configure the scheduling configuration of the process, through a cron rule, through the
  _batchConfiguration.TransactionFilterBatch.cron_ property, or through the environment variable _ACQ_BATCH_INPUT_CRON_

- Apply any other changes to the configuration parameters, the full list of properties is described in __Appendix 2 - Configuration
  properties__

- Run the batch. The batch can be started via the java command:

  >java -jar <nome-jar> --spring.config.location=<location batch files>

  __Nota:__ replace with the path to the proper configuration directory

  >java -jar batch-transaction-filter.jar --spring.config.location=C:\Development\batch-transaction-file\property\
  
  For the bundle execution, referring to the structure already present, execute:
  	
  >java -jar batch-transaction-filter.jar --spring.config.location=file:config/

### Appendice 1 - Public PGP Key

For any problem relating to the use of the public key and for the release of the specifications and / or updates relating to the public
key to be used to encrypt the file, it is mandatory to contact the structure delegated by PagoPA  (ref. SIA OPE Innovative Payments -
[sistemisti_bigdata@sia.eu](mailto:sistemisti_bigdata@sia.eu)). 

__Nota:__ The file filled with the key is included in the bundle containing the artifact for executing the batch.

### Appendice 2 - Configuration properties

#### 1. Logging properties

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------ 
__logging.file__ | Location where the log files will be written || NO
__logging.level.root__ | Log level | INFO | NO | TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF

#### 2. Batch properties - General

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------ 
__batchConfiguration.TransactionFilterBatch.successArchivePath__ | Move initial csv to success path| file:/${ACQ_BATCH_SUCCESS_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/success} | YES
__batchConfiguration.TransactionFilterBatch.errorArchivePath__ | Path where the files whose processing goes wrong are moved | file:/${ACQ_BATCH_ERROR_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/error} | YES
__batchConfiguration.TransactionFilterBatch.cron__ | Batch scheduling | ${ACQ_BATCH_INPUT_CRON:0 0/1 * 1/1 * ?} | YES
__batchConfiguration.TransactionFilterBatch.partitionerMaxPoolSize__ | Batch max partitioner setting | ${ACQ_BATCH_INPUT_PART_MAX_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.partitionerCorePoolSize__ | Batch partitioner pool setup | ${ACQ_BATCH_INPUT_PART_CORE_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.readerMaxPoolSize__ | Maximum number of transaction csv file readers | ${ACQ_BATCH_INPUT_PART_READ_MAX_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.readerCorePoolSize__ | Maximum number of transaction csv file readers | ${ACQ_BATCH_INPUT_PART_READ_CORE_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.tablePrefix__ | Table prefix containing the metadata related to the execution of the batch, if active | ${ACQ_BATCH_INPUT_TABLE_PREFIX:BATCH_} | NO

#### 3. Batch properties - PAN List reading

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------ 
__batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath__ | The path where you saved the file pgp containing HPAN | file:/${ACQ_BATCH_HPAN_INPUT_PATH:}/${ACQ_BATCH_INPUT_FILE_PATTERN:*.csv} | YES
__batchConfiguration.TransactionFilterBatch.panList.secretKeyPath__ | Path where the private key is saved | file:/${ACQ_BATCH_INPUT_SECRET_KEYPATH:} | YES
__batchConfiguration.TransactionFilterBatch.panList.passphrase__ | Passphrase for the private key | ${ACQ_BATCH_INPUT_SECRET_PASSPHRASE:} | YES
__batchConfiguration.TransactionFilterBatch.panList.partitionerSize__ | Size of the partitioner used to read the file | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:1} | NO
__batchConfiguration.TransactionFilterBatch.panList.chunkSize__ | Size of the chunks used for reading the file | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:1} | NO
__batchConfiguration.TransactionFilterBatch.panList.skipLimit__ | Maximum number of records discarded before execution is blocked | ${ACQ_BATCH_INPUT_SKIP_LIMIT:0} | NO
__batchConfiguration.TransactionFilterBatch.panList.applyDecrypt__ | Flag indicating whether or not to apply the decrypt at the hpan file | ${ACQ_BATCH_PAN_LIST_APPLY_DECRYPT:true} | YES | TRUE FALSE	


#### 4. Batch properties - Transaction list reading

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath__ | Path where the transaction file to be processed is read | file:/${ACQ_BATCH_TRX_INPUT_PATH:}/${ACQ_BATCH_INPUT_FILE_PATTERN:*.csv} | YES
__batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath__ | Path where the final file is writtene | file:/${ACQ_BATCH_OUTPUT_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/output} | YES
__batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath__ | Path containing the public key with which to encrypt the result file | file:/${ACQ_BATCH_INPUT_PUBLIC_KEYPATH:} | YES
__batchConfiguration.TransactionFilterBatch.transactionFilter.partitionerSize__ | Partitiner size for transaction files | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:10} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.chunkSize__ | Chunck size for reading transaction files | ${ACQ_BATCH_INPUT_CHUNK_SIZE:1000} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.skipLimit__ | Maximum number of records discarded before execution is blocked | ${ACQ_BATCH_INPUT_SKIP_LIMIT:0} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern__ | Pattern relating to the transaction date | ${ACQ_BATCH_INPUT_TIMESTAMP_PATTERN:MM/dd/yyyy HH:mm:ss} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing__ | Flag that drives the hashing to the pan present in the transaction file | ${ACQ_BATCH_TRX_LIST_APPLY_HASHING:false} | SI | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt__ | Flag to define whether to encrypt the result file | ${ACQ_BATCH_TRX_LIST_APPLY_ENCRYPT:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing__ | Flag to define whether to save the hashing of the pan in the result file | ${ACQ_BATCH_TRX_LIST_HASHING_SAVE:false} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.linesToSkip__ |Number of lines to skip from the beginning of the file (e.g. to avoid the header ) | ${ACQ_BATCH_INPUT_LINES_TO_SKIP:0} | NO

#### 5. Batch properties - SFTP

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionSender.enabled__ | Indicates whether the sending to the sftp channel is active or not | ${ACQ_BATCH_TRX_SENDER_ENABLED:true} | SI | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.localdirectory__ |Local directory from which to get the file to be sent on remote SFTP | ${SFTP_LOCAL_DIR:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.remotedirectory__ | Remote SFTP directory to copy the file to | ${SFTP_REMOTE_DIR:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.filenamepattern__ | Name / pattern of the file to be moved to remote SFTP | ${SFTP_FILE_PATTERN:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.filextension__ | File extension to copy to remote SFTP | ${SFTP_FILE_EXTENSION:} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.host__ | SFTP Host | ${SFTP_HOST:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.port__ | SFTP Port | ${SFTP_PORT:22} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.user__ | User for access to SFTP | ${SFTP_USER:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.password__ | Password for access to SFTP | ${SFTP_PASSWORD:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.timeout__ | Timeout related to connection with SFTP | ${SFTP_SOCKET_TIMEOUT:0:} | SI
__connectors.sftpConfigurations.connection.privateKey__ | Indicates the file for channel authentication will take place via a private key | file:/${SFTP_PRIVATE_KEY:} | NO
__connectors.sftpConfigurations.connection.passphrase__ | Indicates the passphrase associated with the private key | ${SFTP_PASSPHRASE:} | NO

#### 6. Batch properties - Servizi REST

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.saltRecovery.enabled__ | Enable the recovery service for the salt | ${ACQ_BATCH_SALT_RECOVERY_ENABLED:false} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled__ | Enable the recovery service for the pan list | ${ACQ_BATCH_HPAN_RECOVERY_ENABLED:true} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.directoryPath__ | Location where the file containing the list of files will be saved | ${ACQ_BATCH_HPAN_INPUT_PATH:} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.filename__ | Name assigned to the product file | ${CSV_TRX_BATCH_HPAN_LIST_FILENAME:} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.attemptExtract__ | Indication if the recovered file will be in the form of a compressed file with checksum | ${ACQ_BATCH_HPAN_LIST_ATTEMPT_EXTRACT:false} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.checksumFilePattern__ | Pattern for the checksum file | ${ACQ_BATCH_HPAN_LIST_CHECKSUM_FILE_PATTERN: .*checksum.* } | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.listFilePattern__ | Pattern for the list containing the pan list | ${CSV_TRX_BATCH_HPAN_LIST_CHECKSUM_FILE_PATTERN: .*\\.csv } | NO
__rest-client.hpan.base-url__ | Base url for REST services | ${HPAN_SERVICE_URL} | NO
__rest-client.hpan.list.url__ | Endpoint pan list service | /list | NO
__rest-client.hpan.salt.url__ | Endpoint salt service | /salt | NO
__rest-client.hpan.mtls.enabled__ | Enable MTLS for salt and pan list services | ${HPAN_SERVICE_MTLS_ENABLED:true} | NO
__rest-client.hpan.key-store.file__ | Path to key-store | file:/${HPAN_SERVICE_KEY_STORE_FILE:} | NO
__rest-client.hpan.key-store.type__ | Key-store type | ${HPAN_SERVICE_KEY_STORE_TYPE:#{null}} | NO
__rest-client.hpan.key-store.algorithm__ | Key-store algorithm | ${HPAN_SERVICE_KEY_STORE_ALGORITHM:#{null}} | NO
__rest-client.hpan.trust-store.file__ | Path to trust-store | file:/${HPAN_SERVICE_TRUST_STORE_FILE:} | NO
__rest-client.hpan.trust-store.type__ | Trust-store type | ${HPAN_SERVICE_TRUST_STORE_TYPE:#{null}} | NO
__rest-client.hpan.trust-store.algorithm__ | Trust-store algorithm | ${HPAN_SERVICE_TRUST_STORE_ALGORITHM:#{null}} | NO

#### 7. Batch properties - Handling File

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionFilter.deleteLocalFiles__ | Flag to drive the deletion of locally generated files (all files related to batch computation) | ${FLAG_DELETE_LOCAL_FILE:true} | SI | TRUE FALSE

#### 8. Batch properties - Repository

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__spring.datasource.driver-class-name__ | Classname for the driver to user | ${BATCH_DB_CLASS_NAME:} | SI
__spring.datasource.url__ | Database connection url | ${BATCH_DB_CONN_URL:} | SI
__spring.datasource.username__ | Database username for login | ${BATCH_DB_USERNAME:} | SI
__spring.datasource.password__ | Database password for user login | ${BATCH_DB_USERNAME:} | SI
__spring.datasource.hikari.schema__ | Database schema | ${BATCH_DB_SCHEMA:} | SI
__spring.jpa.database-platform__ | Database dialect | ${BATCH_DB_DIALECT:} | SI
