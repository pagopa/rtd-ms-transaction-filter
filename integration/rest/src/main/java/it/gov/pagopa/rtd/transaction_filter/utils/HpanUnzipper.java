package it.gov.pagopa.rtd.transaction_filter.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.Builder;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

@Builder
public class HpanUnzipper {

  private int zipThresholdEntries;
  private long thresholdSizeUncompressed;
  private File fileToUnzip;
  private Path outputDirectory;
  private Predicate<String> isFilenameValidPredicate;
  private String listFilePattern;


  @SneakyThrows
  public File extractZipFile() {

    File localTempFile = null;

    try (ZipFile zipFile = new ZipFile(fileToUnzip)) {

      Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

      long totalSizeArchive = 0;
      int totalEntryArchive = 0;

      while (enumeration.hasMoreElements()) {

        ZipEntry zipEntry = enumeration.nextElement();
        File newFile = new File(
            outputDirectory.toFile().getAbsolutePath() +
                File.separator + zipEntry.getName());

        if (isFilenameValidPredicate.negate().test(zipEntry.getName())) {
          throw new IOException("Illegal filename in archive: " + zipEntry.getName());
        }

        totalEntryArchive++;
        totalSizeArchive += zipEntry.getSize();

        if (totalSizeArchive > thresholdSizeUncompressed) {
          // the uncompressed data size is too much for the application resource capacity
          throw new IOException("The uncompressed data size is over the maximum size allowed.");
        }

        if (totalEntryArchive > zipThresholdEntries) {
          // too many entries in this archive, can lead to inodes exhaustion of the system
          throw new IOException("Too many entries in the archive.");
        }

        try (InputStream zipEntryIS = zipFile.getInputStream(zipEntry);
            FileOutputStream tempFileFOS = new FileOutputStream(newFile)) {

          new File(newFile.getParent()).mkdirs();

          IOUtils.copy(zipEntryIS, tempFileFOS);

          if (zipEntry.getName().matches(listFilePattern)) {
            localTempFile = newFile;
          }
        }
      }
    }
    return localTempFile;
  }
}
