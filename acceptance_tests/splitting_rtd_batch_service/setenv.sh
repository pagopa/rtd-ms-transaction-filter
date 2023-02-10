export ACQ_BATCH_TRX_SENDER_RTD_ENABLED=true
export ACQ_BATCH_TRX_SENDER_ADE_ENABLED=false
export ACQ_BATCH_TRX_SENDER_PENDING_ENABLED=false

export ACQ_BATCH_DELETE_OUTPUT_FILE=KEEP
export ACQ_BATCH_WRITER_RTD_SPLIT_THRESHOLD=400
# chunk size must be 1/4 (or less) the threshold
export ACQ_BATCH_INPUT_CHUNK_SIZE=100

# Disable fetching of HPANs from remote service
export ACQ_BATCH_HPAN_RECOVERY_ENABLED=false
# Avoid delete local resource
export ACQ_BATCH_HPAN_ON_SUCCESS=KEEP
