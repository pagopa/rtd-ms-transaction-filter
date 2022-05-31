package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.connector.AbiToFiscalCodeRestClient;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Implementation of the {@link Tasklet}, recovers a map containing the conversion from
 * acquirer fake abi to fiscal code from a REST service, when enabled.
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class AbiToFiscalCodeMapRecoveryTasklet implements Tasklet {

  private final AbiToFiscalCodeRestClient restClient;
  private final StoreService storeService;
  private boolean taskletEnabled = false;

  /**
   * Recovers a map containing the conversion from acquirer fake abi to fiscal code.
   *
   * @return task exit status
   */
  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

    if (taskletEnabled) {
      log.info("Retrieving acquirer fake abi to fiscal code map");
      Map<String, String> abiToFiscalCodeMap = restClient.getFakeAbiToFiscalCodeMap();
      if (abiToFiscalCodeMap != null) {
        storeService.setAbiToFiscalCodeMap(abiToFiscalCodeMap);
      }
    }

    return RepeatStatus.FINISHED;
  }
}
