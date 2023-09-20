package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import java.io.IOException;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * implementation of the {@link Tasklet}, recovers the pan list from a REST service,
 * when enabled
 */

@Slf4j
@Data
public class HpanListRecoveryTasklet implements Tasklet, InitializingBean {

    private HpanConnectorService hpanConnectorService;
    private String hpanListDirectory;
    private String hpanFilePattern;
    private String fileName;
    private Boolean dailyRemovalTaskletEnabled = false;
    private Boolean recoveryTaskletEnabled = false;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * Recovers a file containing the pan list, and optionally applies checksum
     * validation, and extracts the content from a compressed file, if required
     * @param stepContribution
     * @param chunkContext
     * @return task exit status
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
        throws IOException {

        hpanListDirectory = hpanListDirectory.replace("\\", "/");
        Resource[] resources = null;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String currentDate = OffsetDateTime.now().format(fmt);

        if (Boolean.TRUE.equals(dailyRemovalTaskletEnabled)) {
            deleteHpanFilesOlderThan(currentDate, fmt);
        }

        if (Boolean.TRUE.equals(recoveryTaskletEnabled)) {
            resources = resolver.getResources("file:"
                    .concat(hpanListDirectory)
                    .concat("/")
                    .concat(hpanFilePattern));
            File outputFile = FileUtils.getFile(hpanListDirectory
                    .concat("/".concat(currentDate.concat("_")
                            .concat(fileName != null ? fileName : "hpanList"))));
            if (resources.length == 0 || !outputFile.exists()) {
                File hpanListTempFile = hpanConnectorService.getHpanList();
                FileUtils.moveFile(
                        hpanListTempFile,
                        outputFile);
                hpanConnectorService.cleanAllTempFiles();
            }
        }
        return RepeatStatus.FINISHED;
    }

    private void deleteHpanFilesOlderThan(String currentDate, DateTimeFormatter dateTimeFormatter) {
        Resource[] resources = retrieveExistingHpanFiles();

        try {
            for (Resource resource : resources) {
                BasicFileAttributes fileAttributes = Files.readAttributes(
                    resource.getFile().toPath(), BasicFileAttributes.class);
                long fileLastModTime = fileAttributes.lastModifiedTime().toMillis();
                Instant instant = Instant.ofEpochMilli(fileLastModTime);
                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                String fileLastModifiedDate = localDateTime.format(dateTimeFormatter);
                if (!fileLastModifiedDate.equals(currentDate)) {
                    Files.delete(resource.getFile().toPath());
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @SneakyThrows
    private Resource[] retrieveExistingHpanFiles() {
        return resolver.getResources("file:/"
            .concat(hpanListDirectory.charAt(0) == '/' ?
                hpanListDirectory.replaceFirst("/","") : hpanListDirectory)
            .concat("/")
            .concat(hpanFilePattern));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resolver.getResources(hpanListDirectory),
                "directory must be set");
    }
}
