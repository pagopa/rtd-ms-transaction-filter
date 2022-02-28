# Acquirer Transaction Filter Batch

Component defining the batch process for filtering the input transaction records, based on a list of pan,
recovered from a local directory or through a remote service. 

### Acquirer Integration with PagoPA Centro Stella

The up-to-date integration guide is maintained here:

[Acquirer Integration with PagoPA Centro Stella - Integration](https://app.gitbook.com/o/KXYtsf32WSKm6ga638R3/s/A5nRaBVrAjc1Sj7y0pYS/acquirer-integration-with-pagopa-centrostella/integration)

### Execution requirements

The artifact consists of an executable jar produced with _spring-boot_, therefore all the project dependencies are contained within the jar, together with the classes that contains the business logic.
The artifact is completely autonomous and usable on any device that has a JVM

To install and run the batch, it's required:
- _Java 1.8+_
- _rtd-ms-transaction-filter-<VERSION>-FATJAR.jar_ artifact

To produce the artifact from the source code it will be necessary to have an installation of Maven and a java compiler (jdk1.8+).

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
If there is no particular configuration: an in- memory instance will be executed to allow the batch to be executed.
Please refer to the properties in __Appendix 2 - Configuration properties__ to define the usage of a persistent database.

__Note:__ As of today, Oracle RDBMS are subject to potential issues regarding the isolation level, as reported
in the following [Issue Thread](https://github.com/spring-projects/spring-batch/issues/1127), the general approach to
attempt to avoid similar problems, is to reduce the isolation level defined in the Spring Batch jobRepository
to either _ISOLATION_READ_COMMITTED_, or _ISOLATION_READ_UNCOMMITTED_. this is doable using the configuration property
_batchConfiguration.TransactionFilterBatch.isolationForCreate_.

### Execution methods

Available execution methods are either: single-execution, or scheduled execution. This configuration is defined through
the property _spring.batch.job.scheduled_, a true/false property to define the execution mode, the default value is
configured for a scheduled execution. In case of a scheduled execution, the process will restart when matching
the cron expression defined in the property _batchConfiguration.TransactionFilterBatch.cron_.

__Note__: The property _spring.batch.job.enabled_ must as to be configured to false, when overriding the default
property configurations.

### Logging Configurations

By default, only records that are either filtered, or have been stopped due to an error are logged, while records that
have been processed normally are only logged if one of the following properties is enabled for the read, process, or writing phase:

__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterReadLogging__
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessLogging__
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterWriteLogging__

The frequency at which the normally processed records are logged is set up by the
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.loggingFrequency__, above a frequency
of one record, the logs will be at the INFO level, while at 1 the log will be at DEBUG level.

If is necessary to set up this logging, if there are constraints in terms of data volume, or performance, a suggested
value of frequency would be around 10% of the normal record volume per file.

Active logging/file writing in case of filtered records, or errors, can be disabled through the properties reported
in __Appendix 2, Paragraph 4 - Batch properties - Transaction list reading__.

### Minimal Configuration on Override

When using a customized configuration file, the following minimal configuration has to be maintained, in order to have the correct setup for the batch execution.

![Minimal_Config](readme_screens/Minimal_config.PNG)

### Appendix 2 - Configuration properties

#### 1. Logging properties

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__logging.file__ | Location where the log files will be written || NO
__logging.level.root__ | Log level | INFO | NO | TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF

#### 2. Batch properties - General

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__spring.batch.job.enabled__ | To avoid unmanaged executions, must be at false | FALSE | YES | FALSE
__spring.batch.job.scheduled__ | To define if the process will remain active with scheduled executions | FALSE | YES | TRUE/FALSE
__batchConfiguration.TransactionFilterBatch.successArchivePath__ | Move initial csv to success path| file:/${ACQ_BATCH_SUCCESS_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/success} | YES
__batchConfiguration.TransactionFilterBatch.errorArchivePath__ | Path where the files whose processing goes wrong are moved | file:/${ACQ_BATCH_ERROR_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/error} | YES
__batchConfiguration.TransactionFilterBatch.cron__ | Batch scheduling | ${ACQ_BATCH_INPUT_CRON:0 0/1 * 1/1 * ?} | YES
__batchConfiguration.TransactionFilterBatch.partitionerMaxPoolSize__ | Batch max partitioner setting | ${ACQ_BATCH_INPUT_PART_MAX_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.partitionerCorePoolSize__ | Batch partitioner pool setup | ${ACQ_BATCH_INPUT_PART_CORE_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.readerMaxPoolSize__ | Maximum number of transaction csv file readers | ${ACQ_BATCH_INPUT_PART_READ_MAX_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.readerCorePoolSize__ | Maximum number of transaction csv file readers | ${ACQ_BATCH_INPUT_PART_READ_CORE_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.tablePrefix__ | Table prefix containing the metadata related to the execution of the batch, if active | ${ACQ_BATCH_INPUT_TABLE_PREFIX:BATCH_} | NO
__batchConfiguration.TransactionFilterBatch.isolationForCreate_ | Define the isolation level used by the jobRepository on the batch tables | ${ACQ_BATCH_TRX_ISOLATION_FOR_CREATE:ISOLATION_SERIALIZABLE} | NO

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
__batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath__ | Path where the transaction file to be processed is read | file:/${ACQ_BATCH_TRX_INPUT_PATH:}/ | YES
__batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath__ | Path where the final file is writtene | file:/${ACQ_BATCH_OUTPUT_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/output} | YES
__batchConfiguration.TransactionFilterBatch.transactionFilter.partitionerSize__ | Partitiner size for transaction files | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:10} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.chunkSize__ | Chunck size for reading transaction files | ${ACQ_BATCH_INPUT_CHUNK_SIZE:1000} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.skipLimit__ | Maximum number of records discarded before execution is blocked | ${ACQ_BATCH_INPUT_SKIP_LIMIT:0} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern__ | Pattern relating to the transaction date | ${ACQ_BATCH_INPUT_TIMESTAMP_PATTERN:MM/dd/yyyy HH:mm:ss} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing__ | Flag that drives the hashing to the pan present in the transaction file | ${ACQ_BATCH_TRX_LIST_APPLY_HASHING:false} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt__ | Flag to define whether to encrypt the result file | ${ACQ_BATCH_TRX_LIST_APPLY_ENCRYPT:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.linesToSkip__ | Number of lines to skip from the beginning of the file (e.g. to avoid the header ) | ${ACQ_BATCH_INPUT_LINES_TO_SKIP:0} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.transactionLogsPath__ | Path where the processed transaction records resulting in either an error, or getting filtered, are traced in .csv format |  file:/${ACQ_BATCH_TRX_LOGS_PATH:} | YES
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterReadLogging__ | Property to enable logging for the read records | ${ACQ_BATCH_TRX_AFTER_READ_LOGGING_ENABLED:false} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorLogging__ | Property to enable logging for the records that had errors on the reading phase | ${ACQ_BATCH_TRX_READ_ERROR_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorFileLogging__ | Property to enable writing the records that had errors on the reading phase | ${ACQ_BATCH_TRX_READ_ERROR_FILE_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessLogging__ | Property to enable logging for the processed records | ${ACQ_BATCH_TRX_AFTER_PROCESS_LOGGING_ENABLED:false} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging__ | Property to enable writing the records that had been filtered | ${ACQ_BATCH_TRX_AFTER_PROCESS_FILE_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorLogging__ | Property to enable logging for the records that had errors on the processing phase | ${ACQ_BATCH_TRX_PROCESS_ERROR_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorFileLogging__ | Property to enable writing the records that had errors on the processing phase | ${ACQ_BATCH_TRX_PROCESS_ERROR_FILE_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterWriteLogging__ | Property to enable logging for the written records | ${ACQ_BATCH_TRX_AFTER_WRITE_LOGGING_ENABLED:false} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorLogging__ | Property to enable logging for the records that had errors on the writing phase | ${ACQ_BATCH_TRX_WRITE_ERROR_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorFileLogging__ | Property to enable writing the records that had errors on the writing phase | ${ACQ_BATCH_TRX_WRITE_ERROR_FILE_LOGGING_ENABLED:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.loggingFrequency__ | Logging frequency for transaction records | ${ACQ_BATCH_TRX_READ_LOGGING_FREQUENCY:10000} | YES

#### 6. Batch properties - REST services

Key | Description                                                                                     | Default                                                                 | Mandatory | Values
--- |-------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------| ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled__ | Indicates whether the sending of AdE filtered transactions to the rest channel is active or not | ${ACQ_BATCH_TRX_SENDER_ADE_ENABLED:true}                                | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionSenderRtd.enabled__ | Indicates whether the sending of filtered transactions to the rest channel is active or not     | ${ACQ_BATCH_TRX_SENDER_RTD_ENABLED:true}                                | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.saltRecovery.enabled__ | Enable the recovery service for the salt                                                        | ${ACQ_BATCH_SALT_RECOVERY_ENABLED:false}                                | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled__ | Enable the recovery service for the hpan list                                                   | ${ACQ_BATCH_HPAN_RECOVERY_ENABLED:true}                                 | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.directoryPath__ | Location where the file containing the list of files will be saved                              | ${ACQ_BATCH_HPAN_INPUT_PATH:}                                           | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.filename__ | Name assigned to the recovered file                                                             | ${CSV_TRX_BATCH_HPAN_LIST_FILENAME:}                                    | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.dailyRemoval.enabled__ | Enable daily removal of retrieved hpan files                                                    | ${ACQ_BATCH_HPAN_RECOVERY_DAILY_REM_ENABLED:false}                      | NO | TRUE FALSE
__rest-client.hpan.base-url__ | Base url for REST services                                                                      | ${HPAN_SERVICE_URL}                                                     | NO
__rest-client.hpan.api.key__ | Subscription key to be used if calling Azure-hosted API methods                                 | ${HPAN_API_KEY}                                                         | NO
__rest-client.hpan.list.attemptExtraction__ | Considers the downloaded file as compressed, and attempts an estraction                         | TRUE                                                                    | NO | TRUE FALSE
__rest-client.hpan.list.checksumValidation__ | Attempts to validate the downloaded file using a checksum                                       | TRUE                                                                    | NO | TRUE FALSE
__rest-client.hpan.list.checksumHeaderName__ | Response header containing the file's checksum                                                  | x-ms-meta-sha256                                                        | NO
__rest-client.hpan.list.listFilePattern__ | Pattern to be used for extracting the correct file from the compressed resource                 | *\\.csv                                                                 | NO
__rest-client.hpan.list.dateValidation__ | Enables date validation for the recovered resource                                              | TRUE                                                                    | NO | TRUE FALSE
__rest-client.hpan.list.dateValidationHeaderName__ | Response header containing the file's creation/update date                                      | last-modified                                                           | NO
__rest-client.hpan.list.dateValidationPattern__ | Response header date timestamp pattern (defaults to RFC-1123)                                   |                                                                         | NO
__rest-client.hpan.list.dateValidationZone__ | Zone to consider when validating the creation date for the downloaded file                      | Europe/Rome                                                             | NO
__rest-client.hpan.proxy.enabled__ | Use a Proxied Client                                                                            | ${HPAN_SERVICE_PROXY_ENABLED:false}                                     | NO
__rest-client.hpan.proxy.host__ | Proxy host                                                                                      | ${HPAN_SERVICE_PROXY_HOST:}                                             | NO
__rest-client.hpan.proxy.port__ | Proxy port                                                                                      | ${HPAN_SERVICE_PROXY_PORT:}                                             | NO
__rest-client.hpan.proxy.username__ | Proxy username                                                                                  | ${HPAN_SERVICE_PROXY_USERNAME:}                                         | NO
__rest-client.hpan.proxy.password__ | Proxy password                                                                                  | ${HPAN_SERVICE_PROXY_PASSWORD:}                                         | NO
__rest-client.hpan.mtls.enabled__ | Enable MTLS for salt and pan list services                                                      | ${HPAN_SERVICE_MTLS_ENABLED:true}                                       | NO
__rest-client.hpan.key-store.file__ | Path to key-store                                                                               | file:/${HPAN_SERVICE_KEY_STORE_FILE:}                                   | NO
__rest-client.hpan.key-store.type__ | Key-store type                                                                                  | ${HPAN_SERVICE_KEY_STORE_TYPE:#{null}}                                  | NO
__rest-client.hpan.key-store.algorithm__ | Key-store algorithm                                                                             | ${HPAN_SERVICE_KEY_STORE_ALGORITHM:#{null}}                             | NO
__rest-client.hpan.key-store.password__ | Key-store password                                                                              | ${HPAN_SERVICE_KEY_STORE_PASSWORD:}                                     | NO
__rest-client.hpan.trust-store.file__ | Path to trust-store                                                                             | file:/${HPAN_SERVICE_TRUST_STORE_FILE:}                                 | NO
__rest-client.hpan.trust-store.type__ | Trust-store type                                                                                | ${HPAN_SERVICE_TRUST_STORE_TYPE:#{null}}                                | NO
__rest-client.hpan.trust-store.password__ | Trust-store password                                                                            | ${HPAN_SERVICE_TRUST_STORE_PASSWORD:}                                   | NO
__feign.client.config.hpan-service.connectTimeout__ | Rest client connection timeout, defined in milliseconds                                         | ${REST_CLIENT_CONNECT_TIMEOUT:${HPAN_REST_CLIENT_CONNECT_TIMEOUT:5000}} | NO
__feign.client.config.hpan-service.readTimeout__ | Rest client read timeout, defined in milliseconds                                               | ${REST_CLIENT_READ_TIMEOUT:${HPAN_REST_CLIENT_READ_TIMEOUT:5000}}       | NO


#### 7. Batch properties - File Handling

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionFilter.deleteProcessedFiles__ | Enable deletion of any processed file (all files related to a batch computation) | ${ACQ_BATCH_DELETE_LOCAL_FILE:true} | YES | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.deleteOutputFiles__ | Define output files management rule | ${ACQ_BATCH_DELETE_OUTPUT_FILE:ERROR} | YES | ALWAYS ERROR KEEP
__batchConfiguration.TransactionFilterBatch.transactionFilter.manageHpanOnSuccess__ | Define HPAN files management rule on success | ${ACH_BATCH_HPAN_ON_SUCCESS:DELETE} | YES | DELETE ARCHIVE KEEP

#### 8. Batch properties - Repository

Key |  Description | Default | Mandatory | Values
--- | ------------ | ------- | ------------ | ------
__spring.datasource.driver-class-name__ | Classname for the driver to user | ${ACQ_BATCH_DB_CLASS_NAME:} | YES
__spring.datasource.url__ | Database connection url | ${ACQ_BATCH_DB_CONN_URL:} | YES
__spring.datasource.username__ | Database username for login | ${ACQ_BATCH_DB_USERNAME:} | YES
__spring.datasource.password__ | Database password for user login | ${ACQ_BATCH_DB_PASSWORD:} | YES
__spring.datasource.hikari.schema__ | Database schema | ${ACQ_BATCH_DB_SCHEMA:} | YES
__spring.jpa.database-platform__ | Database dialect | ${ACQ_BATCH_DB_DIALECT:} | YES

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
while the ones with the _"worker-step"_ suffix are related to a single file.

The _status_ and _exit_code_ are useful to determine the general outcome for a step, the first one simply refers to
the general outcome, generally either referring to a __COMPLETED__ or __FAILED__ status, while the second property might
indicate a more detailed code, as an example, if during the processing of a file the fault tolerance is configured, and
some records are skipped, the exit status with reflect this condition with the __COMPLETED_WITH_SKIPS__ exit code.

The table contains a series of columns containing useful information on the processed records: the _read_count_ value
details the number of successfully read lines, while the _read_skip_count_ define the records that are skipped under the
selected fault-tolerance policy. similarly, the _write_count_, _write_skip_count_ and _process_skip_count_ all retain
a similar purpose in the filtering process and writing phases for the record. All the records that are in an error state,
are included in the value for the _rollback_count_.

For the transaction filtering, the _filter_count_ record is useful to keep track of how many transactions have
been filtered, due to not matching the stored HPAN records. Filtered records are by default not included in the rollback
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

As an example, a simple way to route all the produced logs from the console to a specific file, can be easily done
by inserting a simple configuration within the _application.yaml_ file, using the following configuration properties

>logging.file="<path_to_log>/application.log"
>logging.pattern=
>logging.console=

This simple configuration will ensure that all the produced logs, under a general pattern, will be produced on the
configured logfile, instead of being produced withing the console.

If another configuration is required, it is possible to provide a custom configuration, with another set of appenders
defined. As an example, a configuration can be set to write the log records within a defined database schema,
referencing the table structures defined in the [Logback Appenders Documentation](http://dev.cs.ovgu.de/java/logback/manual/appenders.html#DBAppender).

To set a logback configuration we have to provide a _spring-logback.xml_, similar to the following, defined for
a simple configuration for the DBAppender

![Logback_Screen](readme_screens/Logback_DB_Screen.PNG)

To use this configuration in spring, this property must be used within the _application.yaml_ file:

> logging.config=<path_to_config_file>

In order to use this appender, the tables reported in the documentation are required before any execution. The main table,
where the log records are inserted is the __logging_event__ table.

![Logback_Event_DB_Screen](readme_screens/Logback_DB_Table_Screen.PNG)