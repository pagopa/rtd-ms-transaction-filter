package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;


import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Data
public class HpanListRecoveryTasklet implements Tasklet, InitializingBean {

    private HpanConnectorService hpanConnectorService;
    private String hpanListDirectory;
    private String fileName;
    private Boolean taskletEnabled = false;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (taskletEnabled) {
            File outputFile = FileUtils.getFile(hpanListDirectory
                    .concat("/".concat(fileName != null ? fileName : "hpanList")));
            if (!outputFile.exists()) {
                File hpanListTempFile = hpanConnectorService.getHpanList();
                FileUtils.moveFile(
                        hpanListTempFile,
                        outputFile);
            }
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resolver.getResources(hpanListDirectory),
                "directory must be set");
    }
}
