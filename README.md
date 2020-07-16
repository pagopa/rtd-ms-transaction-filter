# Acquirer Transaction Filter Batch

Component defining the batch process for filtering the input transaction records, based on a list of pan,
recovered from a local directory or through a remote service. 

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
the bundle uses this mode for greater immediacy of use. If you want to set it please refer to the properties in __Appendix 2 -
Configuration properties__.

### REST Services Connection

The Batch Acquirer is configurabile for contacting the salt recovery service, to be applied for the PAN hashing,
and the pan list recovery service, to be used for filtering the transaction records. To enabled this services,
the following configuration properties must be enabled: _batchConfiguration.TransactionFilterBatch.saltRecovery.enabled_ 
and _batchConfiguration.TransactionFilterBatch.hpanList.enabled_. 

Endpoint configurations are through the properties _rest-client.hpan.base-url_, for the base configuration
, and the endpoint properties for the two services respectively _rest-client.hpan.list.url_ and _rest-client.hpan.salt.url_.

If the client is to be configured for TLS/SSL protocol usage, the configuration property 
_rest-client.hpan.mtls.enabled_ is to be used, and the keystore and trust-store files for the client, to be applied
in the certificate exchange, respectively through the _rest-client.hpan.key-store.file_ and
_rest-client.hpan.trust-store.file_ properties. 

Other applicable configurations are those related to passwords to be applied for certificates,
identified by the _rest-client.hpan.key-store.password_ and _rest-client.hpan.trust-store.password_ configurations.
The type used for files containing certificates can also be defined,
and the algorithm used for the encryption. By default the files are in Java's JKS format,
using the standard implementation of the X509 algorithm. For dedicated configurations refer to
properties listed in __Appendix 2 - Configuration properties__.

For references to the services displayed through Azure's API service, you can find the corresponding links in 
__Appendix 3 - Authentication Services Acquirer__.

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
   
   __Note:__ The configuration is strictly needed only if the encryption function of the produced files is enabled. In the case of
   configuration on file, the path must be preceded by the prefix _file:/_. for example:
   
    >batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath = file:/C/:Development/keys/public.asc

- Configure the pointing to the file containing the private key, through the property
  _batchConfiguration.TransactionFilterBatch.panList.secretKeyPath_, or through the environment variable
  _ACQ_BATCH_INPUT_SECRET_KEYPATH_. 

  __Note:__ The configuration is strictly necessary only if the decryption function of the files containing the pan list is enabled. 
  In the case of configuration on file, the path must be preceded by prefix _file:/_. for example::

    >batchConfiguration.TransactionFilterBatch.panList.secretKeyPath = file:/C:/Development/keys/secret.asc
	
- Configure the passphrase to be applied if the secret key is enabled, through the
  _batchConfiguration.TransactionFilterBatch.panList.passphrase_ property , or via the _ACQ_BATCH_INPUT_SECRET_PASSPHRASE_ environment
  variable.         

- Define a folder where the path files, to be processed, will be placed

- Configure the path to the transaction files to be processed, through the
  _batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath_ property, or through the environment variables
  _ACQ_BATCH_TRX_INPUT_PATH_ for the folder, and _ACQ_BATCH_INPUT_FILE_PATTERN_, for the pattern of files to read. 

  __Note:__  In the case of file configuration, the path must be preceded by the prefix _file:/_. for example: 

  >batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath = file:/C:/Development/transactions/*.csv

- Define a folder for the files containing the PAN list 

- Configure the path to the files containing the pan list, through the
 _batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath_ property , or through the environment variables
 _ACQ_BATCH_HPAN_INPUT_PATH_ for the folder, and  _ACQ_BATCH_HPAN_INPUT_FILE_PATTERN_, for the pattern of files to read.
 
  __Note:__  In the case of configuration on file, the path must be preceded by the prefix _file:/_. for example: 

  >batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath = file:/C:/Development/hpan/*.pgp

- Define a folder for the output files

- Configure the pointing to the trace files to be processed, through the property
  _batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath_, or through the environment variable
  _ACQ_BATCH_OUTPUT_PATH_      

  __Note:__  In the case of configuration on file, the path must be preceded by the prefix _file:/_. for example:

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
  
- To send the product file on SFTP channel, the functionality must be enabled through 
  _batchConfiguration.TransactionFilterBatch.transactionSender.enabled_ properties,
  then the configurations related to the host, the user used and the authentication method,
  password-based, or through certificate must be reported. Configurations for sftp are listed under the 
  _batchConfiguration.TransactionFilterBatch.transactionFilter.sftp_ root in the configuration properties appendix.
  
- To enable the passages related to the jump recovery services, or the pan list through REST services,
  configure the properties following the definitions in the section __Connecting to REST Services__.    

- Configure the process scheduling, through a cron rule, through the
  _batchConfiguration.TransactionFilterBatch.cron_ property, or through the environment variable _ACQ_BATCH_INPUT_CRON_

- To configure the batch process for the usage of a persisted database, define the configuration parameters under the
  path prefix _spring.datasource_, refer to the command later defined to add the appropriate driver .jar to the process
  classpath, in order to use the chosen database.

- Apply any other changes to the configuration parameters, the full list of properties is described in __Appendix 2 - Configuration
  properties__

- Run the batch. The batch can be started via the java command:

  >java -jar <nome-jar> --spring.config.location=<location batch files>

  __Note:__ replace with the path to the proper configuration directory

  >java -jar batch-transaction-filter.jar --spring.config.location=C:\Development\batch-transaction-file\property\
  
  For the bundle execution, referring to the structure already present, execute:
  	
  >java -jar batch-transaction-filter.jar --spring.config.location=file:config\
                
  For a batch process that does not use the default in-memory database, execute the following command:
  
  >java -jar batch-transaction-filter.jar --spring.config.location=file:config -cp <VENDOR_SPECIFIC_JDBC_DRIVER.jar>
                                                                                                                                                                                                                                                                                                                                                                                                                              
### Appendix 1 - Public PGP Key

For any problem relating to the use of the public key and for the release of the specifications and / or updates relating to the public
key to be used to encrypt the file, it is mandatory to contact the structure delegated by PagoPA  (ref. SIA OPE Innovative Payments -
[sistemisti_bigdata@sia.eu](mailto:sistemisti_bigdata@sia.eu)). 

__Nota:__ The file filled with the key is included in the bundle containing the artifact for executing the batch.

### Appendix 2 - Configuration properties

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

#### 6. Batch properties - REST services

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.saltRecovery.enabled__ | Enable the recovery service for the salt | ${ACQ_BATCH_SALT_RECOVERY_ENABLED:false} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled__ | Enable the recovery service for the pan list | ${ACQ_BATCH_HPAN_RECOVERY_ENABLED:true} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.directoryPath__ | Location where the file containing the list of files will be saved | ${ACQ_BATCH_HPAN_INPUT_PATH:} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.filename__ | Name assigned to the recovered file | ${CSV_TRX_BATCH_HPAN_LIST_FILENAME:} | NO
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
__rest-client.hpan.key-store.password__ | Key-store password | ${HPAN_SERVICE_KEY_STORE_PASSWORD:} | NO
__rest-client.hpan.trust-store.file__ | Path to trust-store | file:/${HPAN_SERVICE_TRUST_STORE_FILE:} | NO
__rest-client.hpan.trust-store.type__ | Trust-store type | ${HPAN_SERVICE_TRUST_STORE_TYPE:#{null}} | NO
__rest-client.hpan.trust-store.password__ | Trust-store password | ${HPAN_SERVICE_TRUST_STORE_PASSWORD:} | NO


#### 7. Batch properties - File handling

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionFilter.deleteLocalFiles__ | Enable deletion of locally generated files (all files related to batch computation) | ${FLAG_DELETE_LOCAL_FILE:true} | SI | TRUE FALSE

#### 8. Batch properties - Repository

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__spring.datasource.driver-class-name__ | Classname for the driver to user | ${BATCH_DB_CLASS_NAME:} | SI
__spring.datasource.url__ | Database connection url | ${BATCH_DB_CONN_URL:} | SI
__spring.datasource.username__ | Database username for login | ${BATCH_DB_USERNAME:} | SI
__spring.datasource.password__ | Database password for user login | ${BATCH_DB_USERNAME:} | SI
__spring.datasource.hikari.schema__ | Database schema | ${BATCH_DB_SCHEMA:} | SI
__spring.jpa.database-platform__ | Database dialect | ${BATCH_DB_DIALECT:} | SI

### Appendix 3 - Acquirer Services Authentication

The interactions for the Acquirer batch services use a mutual authentication mechanism over TLS/SSL protocol,
through the exchange of public certificates, issued by a CA (the certifying authority), used for the verification
by both actors compared to the keys in their possession.
For this mechanism to be applicable it will therefore be necessary that:

The Client will have to be configured to send requests on TLS/SSL protocol, indicating a file containing the
public certificate issued for the machine that will implement the requests,
and will also need to be configured to receive a collection of keys to be used for verification of the
certificates reported by the car contacted. 

the API must be configured to accept requests on TLS/SSL protocol, it must be configured to use
a collection of keys on which to apply the certificate verification, must be configured to provide an 
public certificate, used by the Client for authentication of the machine to which the request is directed.

Using the services provided on Azure, to enable the authentication process, the following must be entered
certificates relating to the SOs (https://docs.microsoft.com/en-us/azure/api-management/api-management-howto-ca-certificates).
The format of the certificates will in this case be ".cer".

The certificates used in the case of services displayed through Azure, must be included in the dedicated section, 
these must be in the ".pfx" format.  (https://docs.microsoft.com/en-us/azure/api-management/api-management-howto-mutual-certificates). 

The services displayed on Azure will allow the configuration of the backend services displayed so as to enable the
mutual authentication process based on a given certificate. In the case of services used by
Acquirer introduces a dedicated policy to allow the authentication process through multiple certificates,
to allow the use of certificates for Acquirers 
(https://docs.microsoft.com/en-us/azure/api-management/api-management-howto-mutual-certificates-for-clients).

### Appendix 3 - Process Debugging

#### 1. Batch Tables

The process provides varied information to refer to in case of the need to check the results of an execution, 
for debugging or decision purposes. The first tool at disposal is to refer to the data automatically provided from
Spring Batch, through its internal repository tables, where a series of information regarding the executions are
stored. 

An execution can be identified through the table __batch_job_execution__,
which defines an instance of execution of the whole batch, the _job_execution_id will_ be important to search the 
related steps, and the properties _status_ and _exit_code_ defines if the job successfully completed.
In this particular case the codes will generally be on __COMPLETED__ after a job execution is ended,
unless an error outside the scope of the single steps outcomes occurs.

![example of jo execution in DB](/readme_screens/JobExec_DB_Screen.PNG)

To check for the information involving the reading of a file, the table __batch_step_execution__ contains 
the information regarding the single steps of the processes identified by a _job_execution_id_, the steps useful for debugging the file processing steps are the ones containing
in the _step_name_ column, the values _"hpan-recovery"_ or _"transaction-filter"_. The steps with the name containing 
_"master-step"_ as a suffix define the general status of all the related files processed during the execution,
while the ones with the _"worker-step_partion"_ suffix are related to a single file.

The _status_ and _exit_code_ are useful to determine the general outcome for a step, the first one simply refers to
the general outcome, generally either referring to a __COMPLETED__ or __FAILED__ status, while the second property might
indicate a more detailed code, as an example, if during the processing of a file the fault tolerance is configured, and
some records are skipped, the exit status with reflect this condition with the __COMPLETED_WITH_SKIPS__ exit code.

The table contains a series of columns containing useful information on the processed records: the _read_count_ value
details the number of successfully read lines, while the _read_skip_count_ define the records that are skipped under the
selected fault-tolerance policy. Similarly, the _write_count_, _write_skip_count_ and _process_skip_count_ all retain
a similar purpose in the filtering process and writing phases for the record. All the records that are in an error state,
are included in the value for the _rollback_count_.

For the transaction filtering, the _filter_count_ record is useful to keep track of how many transactions have 
been filtered, due to not matching the stored PAN records. Filtered records are by default not included in the rollback 
count, as it's part of the business logic.

![example of execution with skips in DB](/readme_screens/Skips_DB_Screen.PNG)

With the _step_execution_id_ value, we can match the information for the related step in the
__batch_step_execution_context__ table, that contains the execution parameters in the _shor_context_
column. The workers steps context contains the processed file inside the JSON structure, under the 
_"filename"_ property.

![example of execution context_in_DB](/readme_screens/Step_Exec_Context_Screen.PNG)

Further information about the Spring Batch repository entities, can be found in the
[Reference Manual](https://docs.spring.io/spring-batch/docs/current/reference/html/schema-appendix.html#metaDataBatchStepExecutionContext)

#### 2. Console Log

To have a more detailed view for the state of the executions, a series of log entries are provided to trace the execution
instances, defined either by the standard batch logs, or by custom entries to provide some extra details. A general log
regarding the execution of the scheduled polling check can be found as of the provided example, eventually reporting 
the lack of a proper execution, due to missing files in the provided source directory.

![example of console_log_no_file_screen](/readme_screens/ConLog_NoFile_Screen.PNG)

Whenever a file is about to get processed, a log trace can be found, regarding the start of the processing step, and
the related filename.

![example of console_log_start_file_read_screen](/readme_screens/ConLog_StartFile_Screen.PNG)

During the file processing, logs regarding the single records are produced to keep track of any errors,
or filtering occurred. Every log comes with the file name, and line of the record that produced the log entry,
with any additional information. 

![example of console_log_trx_filter_screen](/readme_screens/ConLog_FilteredTrx_Screen.PNG)

Other logs can be used to obtain the general flow for every step of the process and their status, describing the eventual
status and time of completion, this particular information is also reported within the Spring Batch tables, detailed above.

![example of console_log_batch_flow_screen](/readme_screens/ConLog_BatchFlow_Screen.PNG)

#### 3. Logback Configurations

Spring uses default configurations to produce its logs, either using __Log4J2__ or __Logback__, through internal 
configurations it can provide the functionality to use the appenders to produce logs on the stdout channel, or on
a file. This can be achieved using the configuration properties exposed for this purpose, the details of which can
be found in the [Reference Documentation](https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/boot-features-logging.html).

As an example, a simple way to route all the produced logs from the console to a specific file, can be easly done 
by inserting a simple configuration within the _application.yaml_ file, using the following configuration properties

>logging.file="<path_to_log>/application.log"
>logging.pattern=
>logging.console=

This simple configuration will ensure that all the produced logs, under a general pattern, will be produced on the
configured logfile, instead of being produced withing the console.


 




