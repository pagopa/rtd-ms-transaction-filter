# Acquirer Transaction Filter Batch

Component defining the batch process for filtering the input transaction records, based on a list of pan, recovered from a local directory or through a remote service. 

### Requisiti per l’esecuzione

L’artefatto consiste in un jar eseguibile prodotto con spring-boot, pertanto tutte le dipendenze del progetto sono
contenute all’interno del jar insieme alle classi che ne contengono la business logic. In questo modo
l’artefatto è completamente autonomo e utilizzabile su un qualsiasi dispositivo che disponga di una JVM.

Per l’installazione ed esecuzione del batch sono necessari:
- _Java 1.8+_
- _Artefatto batch-transaction-filter.jar_

Per l’applicazione della criptazione PGP al file prodotto in uscita al servizio batch, se abilitata,
sarà necessario che sia inoltre presente un file contenente un file contenente la chiave pubblica da impiegare, 
riportata in Appendice 1 - Chiave pubblica PGP. Per l’applicazione della decriptazione del file dei pan PGP,
se abilitata, dovrà prevedere la presenza di un file contenente la chiave segreta da applicare per l’operazione.
Nel caso si voglia produrre l’artefatto dal sorgente sarà necessario avere un’installazione locale di Maven.

### Distribuzione Bundle

L’artefatto _batch-transaction-filter.jar_ sarà fornito in un bundle contenente una folder contenente le configurazioni
e struttura delle cartelle, in modo da permettere l’esecuzione immediata su configurazione di default, con puntamento 
alle cartelle riportate nel bundle. Il servizio di default eseguirà un polling per verificare la presenza di file
da processare ogni minuto.

All’interno del bundle sono inoltre presenti il file relativo alla chiave pubblica riportata in appendice,
e dei file sample per una prima esecuzione di test.
 
__Nota:__ Il bundle contiene una versione potenzialmente non allineata alle implementazioni del batch.
La configurazione di default è tale da disabilitare i tentativi di connessione ai servizi REST,
ed il servizio per l’invio dei file prodotti su canale SFTP.

La struttura del bundle, ed i file presenti, è la seguente:

- _batch-transaction-filter.jar_, l’artefatto contenente il servizio batch
- _/config_, cartella contenente i file di configurazione
- _/config/application.yml_, file contenente le proprietà di configurazione per il servizio
- _/resources_, cartella contenente le risorse e folder per l’esecuzione del bundle sotto una configurazione di default
- _/hpans_, folder dove inserire i file contenenti la lista di pan
- _/keys_, folder contenente le chiavi per l’applicazione del pgp ai file in lettura/scrittura
- _/transactions_, folder dove inserire i file contenenti le transazioni da processare
- _/output_, folder dove saranno inseriti i file prodotti dal servizio
- _/sample-files_, folder contenente file di prova per l’esecuzione

### Generazione artefatto da sorgente

Per ottenere una versione dell’artefatto prodotta direttamente sulla base del sorgente, è necessario che sulla macchina 
sia opportunamente configurata un’istanza Maven. Una volta scaricato il sorgente, portarsi nella directory radice
di quest’ultima tramite riga di comando, e lanciare il comando:

> mvn clean package __<opzioni_esecuzione>__
	
Nel caso si richiami il comando senza altre opzioni, l’artefatto sarà prodotto una volta eseguiti i test unitari 
dichiarati nel modulo, solo nel caso di una corretta risoluzione. Nel caso si voglia, per eventuali
motivi di tempistiche, eseguire l’operazione senza attendere l’esecuzione e validazione dei test,
eseguire il comando nella forma seguente:

>mvn clean package -DskipTests
	
L’artefatto prodotto sarà recuperabile dalla cartella target, visibile dalla folder di root.

### Connessione al database

Spring Batch utilizza un repository su cui tenere traccia delle esecuzione effettuate dal servizio, nel caso in cui
non ci sia una particolare configurazione un’istanza in-memory sarà eseguita per permettere l’esecuzione del batch.
La configurazione del bundle utilizza questa modalità per maggior immediatezza di utilizzo. 
Se si vuole configurare altrimenti fare riferimento alle proprietà in Appendice 2 - Proprietà di configurazione.

### Linee guida per l’ esecuzione

- Installare e configurare l’ambiente perché sia disponibile la versione Java 1.8, come da indicazione nei prerequisiti

- Nel caso di esecuzione della versione bundled, estrarre artefatto e folder risorse nella posizione scelta,
  se non necessaria alcuna configurazione aggiuntiva fare riferimento al comando riportato al termine di questo paragrafo.

- Nel caso non si stia utilizzando la versione bundled, produrre l’artefatto tramite codice sorgente,
  come indicato nel paragrafo corrispondente del manuale. Preparare un file application.yml di configurazione,
  ed eventuali altri file .yml o .properties da utilizzare per le proprietà coinvolte.

- Posizionare sulla macchina, in una locazione a scelta, l’artefatto batch-transaction-filter.jar

- Posizionare sulla macchina, in una locazione a scelta, i file di configurazione,
  forniti assieme all’artefatto nel bundle, o prodotti direttamente.

- Posizionare sulla macchina, in una locazione a scelta, i file relativi alla chiave pubblica e/o privata per pgp,
  se una delle funzionalità di criptazione/de-criptazione dei file sia attiva.

- Configurare il puntamento al file contenente la chiave pubblica, attraverso la proprietà 
  _batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath_,
   oppure tramite la variabile d’ambiente _ACQ_BATCH_INPUT_PUBLIC_KEYPATH_. 
   
   __Nota:__ La configurazione è strettamente necessaria solo nel caso si utilizzi la funzione di
    criptazione dei file prodotti. Nel caso di configurazione su file, il percorso dovrà essere preceduto
    dal prefisso _file:/_. Ad esempio:

    >batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath = file:/C/:Development/keys/public.asc

- Configurare il puntamento al file contenente la chiave privata, attraverso la proprietà 
  _batchConfiguration.TransactionFilterBatch.panList.secretKeyPath_,
   oppure tramite la variabile d’ambiente _ACQ_BATCH_INPUT_SECRET_KEYPATH_.

  __Nota:__ La configurazione è strettamente necessaria solo nel caso si utilizzi la
   funzione di decriptazione dei file contenenti la lista di pan.
   Nel caso di configurazione su file, il percorso dovrà essere preceduto dal prefisso “file:/”. Ad esempio:

    >batchConfiguration.TransactionFilterBatch.panList.secretKeyPath = file:/C:/Development/keys/secret.asc
	
- Configurare la passphrase da applicare in caso di utilizzo della chiave segreta,
  tramite proprietà _batchConfiguration.TransactionFilterBatch.panList.passphrase_,
  o tramite variabile d’ambiente _ACQ_BATCH_INPUT_SECRET_PASSPHRASE_. 

- Definire una folder dove saranno posizionati i file di tracciato da processare

- Configurare il puntamento ai file di tracciato da processare, attraverso la proprietà
  _batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath_,
  oppure tramite le variabili d’ambiente _ACQ_BATCH_TRX_INPUT_PATH_, per la folder, e 
  _ACQ_BATCH_INPUT_FILE_PATTERN_, per il pattern da rispettare da leggere all’interno della folder. 

  __Nota:__  Nel caso di configurazione su file, il percorso dovrà essere preceduto dal prefisso “file:/”. Ad esempio:

  >batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath = file:/C:/Development/transactions/*.csv

- Definire una folder dove saranno posizionati i file contenenti la lista di pan

- Configurare il puntamento ai file contenenti la lista di pan, attraverso la proprietà
  batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath,
  oppure tramite le variabili d’ambiente _ACQ_BATCH_HPAN_INPUT_PATH_, per la folder, e 
  _ACQ_BATCH_HPAN_INPUT_FILE_PATTERN_, per il pattern da rispettare da leggere all’interno della folder. 

  __Nota:__  Nel caso di configurazione su file, il percorso dovrà essere preceduto dal prefisso “file:/”. Ad esempio:

  >batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath = file:/C:/Development/hpan/*.pgp

- Definire una folder dove saranno prodotti localmente i file in output

- Configurare il puntamento ai file di tracciato da processare,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath_,
  oppure tramite la variabile d’ambiente _ACQ_BATCH_OUTPUT_PATH_

  __Nota:__  Nel caso di configurazione su file, il percorso dovrà essere preceduto dal prefisso “file:/”. Ad esempio:

  >batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath = file:/C:/Development/output

- Configurare l’applicazione dell’hashing per la lista di pan,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.panList.applyHashing_,
  oppure attraverso la variabile d’ambiente _ACQ_BATCH_PAN_LIST_APPLY_HASHING_

- Configurare per decriptazione del file contenente la lista di pan,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.panList.applyDecrypt_,
  oppure attraverso la variabile d’ambiente _ACQ_BATCH_PAN_LIST_APPLY_DECRYPT_

- Configurare l’applicazione dell’hash per le transazioni,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing_,
  oppure attraverso la variabile d’ambiente _ACQ_BATCH_TRX_LIST_APPLY_HASHING_

- Configurare per criptazione dei prodotti,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt_,
  oppure attraverso la variabile d’ambiente _ACQ_BATCH_TRX_LIST_APPLY_ENCRYPT_

- Configurare per l’applicazione dell’hash nelle transazioni riportate nel file prodotto,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing_,
  oppure attraverso la variabile d’ambiente _ACQ_BATCH_TRX_LIST_HASHING_SAVE_

- Configurare la configurazione di schedulazione del processo, tramite una regola cron,
  attraverso la proprietà _batchConfiguration.TransactionFilterBatch.cron_,
  oppure attraverso la variabile d’ambiente _ACQ_BATCH_INPUT_CRON_

- Applicare eventuali altre modifiche ai parametri di configurazione,
  descritti in __Appendice 2 - Proprietà di configurazione__

- Eseguire il batch. Il batch può essere avviato tramite il comando java:

  >java -jar <nome-jar> --spring.config.location=<location batch files>

  __Nota:__ sostituire quanto evidenziato in rosso con le opportune configurazioni. ad esempio:

  >java -jar batch-transaction-filter.jar --spring.config.location=C:\Development\batch-transaction-file\property\
  
  Nel caso di esecuzione su artefatto contenuto nel bundle,
  secondo la struttura delle risorse contenute in quest’ultimo, eseguire:
  	
  >java -jar batch-transaction-filter.jar --spring.config.location=file:config/

### Appendice 1 - Public PGP Key

Per qualsiasi problema relativo all’utilizzo della chiave pubblica e per il rilascio delle specifiche e/o aggiornamento
relativi alla chiave pubblica da utilizzare per cifrare il file è necessario contattare la struttura delegata da PagoPa
di competenza (rif. SIA OPE Innovative Payments - [sistemisti_bigdata@sia.eu](mailto:sistemisti_bigdata@sia.eu)). 

__Nota:__ Il file contenente la chiave viene incluso nel bundle contenente l’artefatto per l’esecuzione del batch.

### Appendice 2 - Proprietà di configurazione

#### 1. Proprietà Logging

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------ 
__logging.file__ | Percorso in cui scrivere il file di log || NO
__logging.level.root__ | Livello di log | INFO | NO | TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF

#### 2. Proprietà Batch - Generali

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------ 
__batchConfiguration.TransactionFilterBatch.successArchivePath__ | Sposta csv iniziale sul path success | file:/${ACQ_BATCH_SUCCESS_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/success} | SI
__batchConfiguration.TransactionFilterBatch.errorArchivePath__ | Path in cui vengono spostati i file la cui elaborazione vanno in errore | file:/${ACQ_BATCH_ERROR_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/error} | SI
__batchConfiguration.TransactionFilterBatch.cron__ | Schedulazione batch | ${ACQ_BATCH_INPUT_CRON:0 0/1 * 1/1 * ?} | SI
__batchConfiguration.TransactionFilterBatch.partitionerMaxPoolSize__ | Impostazione max partitioner del batch | ${ACQ_BATCH_INPUT_PART_MAX_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.partitionerCorePoolSize__ | Impostazione pool di partitioner del batch | ${ACQ_BATCH_INPUT_PART_CORE_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.readerMaxPoolSize__ | Numero massimo di reader del file csv delle transazioni | ${ACQ_BATCH_INPUT_PART_READ_MAX_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.readerCorePoolSize__ | Numero massimo di reader del file csv delle transazioni | ${ACQ_BATCH_INPUT_PART_READ_CORE_POOL_SIZE:5} | NO
__batchConfiguration.TransactionFilterBatch.tablePrefix__ | Prefisso tabelle contenente i metadati relativi all’esecuzione del batch, se attiva | ${ACQ_BATCH_INPUT_TABLE_PREFIX:BATCH_} | NO

#### 3. Proprietà Batch - Lettura lista PAN

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------ 
__batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath__ | Percorso in cui è salvato il file pgp contenente gli HPAN | file:/${ACQ_BATCH_HPAN_INPUT_PATH:}/${ACQ_BATCH_INPUT_FILE_PATTERN:*.csv} | SI
__batchConfiguration.TransactionFilterBatch.panList.secretKeyPath__ | Percorso in cui è salvata la chiave privata | file:/${ACQ_BATCH_INPUT_SECRET_KEYPATH:} | SI
__batchConfiguration.TransactionFilterBatch.panList.passphrase__ | Passphrase per la chiave privata | ${ACQ_BATCH_INPUT_SECRET_PASSPHRASE:} | SI
__batchConfiguration.TransactionFilterBatch.panList.partitionerSize__ | Size dei partitioner utilizzati per la lettura del file | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:1} | NO
__batchConfiguration.TransactionFilterBatch.panList.chunkSize__ | Dimensione dei chunk utilizzati per la lettura del file | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:1} | NO
__batchConfiguration.TransactionFilterBatch.panList.skipLimit__ | Numero massimo di record scartati prima che venga bloccata l’esecuzione | ${ACQ_BATCH_INPUT_SKIP_LIMIT:0} | NO
__batchConfiguration.TransactionFilterBatch.panList.applyDecrypt__ | Flag che indica se applicare o meno la descrittazione al file degli hpan | ${ACQ_BATCH_PAN_LIST_APPLY_DECRYPT:true} | SI | TRUE FALSE

#### 4. Proprietà Batch - Lettura lista transazioni

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath__ | Path in cui viene letto il file delle transazioni da processare | file:/${ACQ_BATCH_TRX_INPUT_PATH:}/${ACQ_BATCH_INPUT_FILE_PATTERN:*.csv} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath__ | dove viene scritto il file finale | file:/${ACQ_BATCH_OUTPUT_PATH:${ACQ_BATCH_TRX_INPUT_PATH:}/output} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath__ | Percorso che contiene la chiave pubblica con cui cifrare il file finale | file:/${ACQ_BATCH_INPUT_PUBLIC_KEYPATH:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.partitionerSize__ | Dimensione partitiner per file transazioni | ${ACQ_BATCH_INPUT_PARTITIONER_SIZE:10} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.chunkSize__ | Dimensione chunck per lettura file transazioni | ${ACQ_BATCH_INPUT_CHUNK_SIZE:1000} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.skipLimit__ | Numero massimo di record scartati prima che venga bloccata l’esecuzione | ${ACQ_BATCH_INPUT_SKIP_LIMIT:0} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern__ | Pattern relativo alla data di transazione | ${ACQ_BATCH_INPUT_TIMESTAMP_PATTERN:MM/dd/yyyy HH:mm:ss} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing__ | Flag che pilota l’applicazione dell’hashing al pan presente nel file transazioni | ${ACQ_BATCH_TRX_LIST_APPLY_HASHING:false} | SI | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt__ | Flag per definire se applicare la crittazione del file prodotto | ${ACQ_BATCH_TRX_LIST_APPLY_ENCRYPT:true} | SI | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing__ | Flag per definire se salvare l’han del pan nel file prodotto | ${ACQ_BATCH_TRX_LIST_HASHING_SAVE:false} | SI | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.linesToSkip__ | Numero di linee da saltare a partire dall’inizio del file (ad es. per evitare l'header) | ${ACQ_BATCH_INPUT_LINES_TO_SKIP:0} | NO

#### 5. Proprietà Batch - SFTP

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionSender.enabled__ | Indica se l’invio verso canale sftp sia o meno attivo | ${ACQ_BATCH_TRX_SENDER_ENABLED:true} | SI | TRUE FALSE
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.localdirectory__ | Directory locale da cui prendere il file da inviare su SFTP remoto | ${SFTP_LOCAL_DIR:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.remotedirectory__ | Directory remota dell’sftp in cui copiare il file | ${SFTP_REMOTE_DIR:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.filenamepattern__ | Nome/pattern del file da spostare su SFTP remoto | ${SFTP_FILE_PATTERN:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.filextension__ | Estensione del file da copiare su sftp remoto | ${SFTP_FILE_EXTENSION:} | NO
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.host__ | Host SFTP | ${SFTP_HOST:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.port__ | Porta SFTP | ${SFTP_PORT:22} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.user__ | Utenza per accesso ad SFTP | ${SFTP_USER:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.password__ | Password per accesso a SFTP | ${SFTP_PASSWORD:} | SI
__batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.timeout__ | Timeout relativo alla connessione con SFTP | ${SFTP_SOCKET_TIMEOUT:0:} | SI
__connectors.sftpConfigurations.connection.privateKey__ | Indica il file per l’autenticazione su canale avverrà tramite chiave privata | file:/${SFTP_PRIVATE_KEY:} | NO
__connectors.sftpConfigurations.connection.passphrase__ | Indica la passphrase associata alla chiave privata | ${SFTP_PASSPHRASE:} | NO

#### 6. Proprietà Batch - Servizi REST

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.saltRecovery.enabled__ | Abilitazione del servizio di recupero per il salt | ${ACQ_BATCH_SALT_RECOVERY_ENABLED:false} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled__ | Abilitazione del servizio di recupero per la lista di pan | ${ACQ_BATCH_HPAN_RECOVERY_ENABLED:true} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.directoryPath__ | Locazione dove sarà salvato il file contente la lista di file | ${ACQ_BATCH_HPAN_INPUT_PATH:} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.filename__ | Nome assegnato al file prodotto | ${CSV_TRX_BATCH_HPAN_LIST_FILENAME:} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.attemptExtract__ | Indicazione se il file recuperato sarà nella forma di un file compresso con checksum | ${ACQ_BATCH_HPAN_LIST_ATTEMPT_EXTRACT:false} | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.checksumFilePattern__ | Pattern per il file di checksum | ${ACQ_BATCH_HPAN_LIST_CHECKSUM_FILE_PATTERN: .*checksum.* } | NO
__batchConfiguration.TransactionFilterBatch.hpanListRecovery.listFilePattern__ | Pattern per la lista contenente la lista di pan | ${CSV_TRX_BATCH_HPAN_LIST_CHECKSUM_FILE_PATTERN: .*\\.csv } | NO
__rest-client.hpan.base-url__ | Base url per i servizi REST | ${HPAN_SERVICE_URL} | NO
__rest-client.hpan.list.url__ | Endpoint per recupero lista pan | /list | NO
__rest-client.hpan.salt.url__ | Endpoint per recupero salt | /salt | NO
__rest-client.hpan.mtls.enabled__ | Abilitazione MTLS per chiamate ai servizi per salt e lista pan | ${HPAN_SERVICE_MTLS_ENABLED:true} | NO
__rest-client.hpan.key-store.file__ | Riferimento a file per key-store | file:/${HPAN_SERVICE_KEY_STORE_FILE:} | NO
__rest-client.hpan.key-store.type__ | Tipo di key-store utilizzato. | ${HPAN_SERVICE_KEY_STORE_TYPE:#{null}} | NO
__rest-client.hpan.key-store.algorithm__ | Tipo di algoritmo utilizzato | ${HPAN_SERVICE_KEY_STORE_ALGORITHM:#{null}} | NO
__rest-client.hpan.trust-store.file__ | Riferimento a file per trust-store | file:/${HPAN_SERVICE_TRUST_STORE_FILE:} | NO
__rest-client.hpan.trust-store.type__ | Tipo di trust-store utilizzato. | ${HPAN_SERVICE_TRUST_STORE_TYPE:#{null}} | NO
__rest-client.hpan.trust-store.algorithm__ | Tipo di algoritmo utilizzato | ${HPAN_SERVICE_TRUST_STORE_ALGORITHM:#{null}} | NO

#### 7. Proprietà Batch - Gestione File

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------
__batchConfiguration.TransactionFilterBatch.transactionFilter.deleteLocalFiles__ | Flag per pilotare la cancellazione dei file generati in locale (tutti i file relativi alla computazione del batch) | ${FLAG_DELETE_LOCAL_FILE:true} | SI | TRUE FALSE

#### 8. Proprietà Batch - Repository

Key |  Description | Default | Obbligatorio | Valori
--- | ------------ | ------- | ------------ | ------
__spring.datasource.driver-class-name__ | Classname per il driver relativo al db da utilizzare | ${BATCH_DB_CLASS_NAME:} | SI
__spring.datasource.url__ | Url per la connessione al db da utilizzare | ${BATCH_DB_CONN_URL:} | SI
__spring.datasource.username__ | Username per la connessione a db | ${BATCH_DB_USERNAME:} | SI
__spring.datasource.password__ | Password per la connessione a db | ${BATCH_DB_USERNAME:} | SI
__spring.datasource.hikari.schema__ | Schema a cui connettersi per il database | ${BATCH_DB_SCHEMA:} | SI
__spring.jpa.database-platform__ | Indicazione del dialetto da utilizzare per il database di riferimento | ${BATCH_DB_DIALECT:} | SI




