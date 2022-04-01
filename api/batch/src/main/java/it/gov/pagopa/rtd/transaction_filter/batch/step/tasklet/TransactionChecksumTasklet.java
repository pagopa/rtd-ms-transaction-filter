package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;

@Data
@Slf4j
public class TransactionChecksumTasklet implements Tasklet {

    private Resource resource;
    private StoreService storeService;
    private boolean taskletEnabled = false;

    @Override
    @SneakyThrows
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (taskletEnabled) {
            try (InputStream is = Files.newInputStream(resource.getFile().toPath())) {
                String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(is);
                log.info("File " + resource.getFilename() + " SHA256 digest: " + sha256hex);
                storeService.storeHash(resource.getFilename(), sha256hex);
            }
        }
        return RepeatStatus.FINISHED;
    }

}
