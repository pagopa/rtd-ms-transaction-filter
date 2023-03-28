package it.gov.pagopa.rtd.transaction_filter.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

@Builder
@Data
public class HpanUnzipper {

  private int zipThresholdEntries;
  private long thresholdSizeUncompressed;
  private File fileToUnzip;
  private Path outputDirectory;
  private Predicate<String> isFilenameValidPredicate;
  private String listFilePattern;
  private double thresholdRatio;


  @SneakyThrows
  public File extractZipFile() {

    File localTempFile = null;

    try (ZipFile zipFile = new ZipFile(fileToUnzip)) {

      Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

      long totalSizeArchive = 0;
      int totalEntryArchive = 0;

      while (enumeration.hasMoreElements()) {

        ZipEntry zipEntry = enumeration.nextElement();
        File outputEntry = new File(
            outputDirectory.toFile().getAbsolutePath() +
                File.separator + zipEntry.getName());

        String destinationCanonicalPath = outputEntry.getCanonicalPath();
        if (!destinationCanonicalPath.startsWith(outputDirectory.toFile().getCanonicalPath())) {
          throw new ZipException(
              "Potential zip slip vulnerability in zip entry: " + zipEntry.getName());
        }

        if (isFilenameValidPredicate.negate().test(zipEntry.getName())) {
          throw new IOException("Illegal filename in archive: " + zipEntry.getName());
        }

        totalEntryArchive++;
        int totalSizeEntry = 0;

        if (totalEntryArchive > zipThresholdEntries) {
          // too many entries in this archive, can lead to inodes exhaustion of the system
          throw new IOException("Too many entries in the archive.");
        }

        try (InputStream zipEntryInputStream = new BufferedInputStream(
            zipFile.getInputStream(zipEntry));
            OutputStream outputStream = new BufferedOutputStream(
                Files.newOutputStream(outputEntry.toPath()))) {
          int byteCopied = IOUtils.copy(zipEntryInputStream, outputStream);
          totalSizeEntry += byteCopied;
          totalSizeArchive += byteCopied;
        }

        double compressionRatio = (double) totalSizeEntry / zipEntry.getCompressedSize();
        if (compressionRatio > thresholdRatio) {
          // ratio between compressed and uncompressed data is highly suspicious, looks like a Zip Bomb Attack
          throw new IOException("Compression ratio is highly suspicious, check the hpan zip archive.");
        }

        if (totalSizeArchive > thresholdSizeUncompressed) {
          // the uncompressed data size is too much for the application resource capacity
          throw new IOException("The uncompressed data size is over the maximum size allowed.");
        }

        if (zipEntry.getName().matches(listFilePattern)) {
          localTempFile = outputEntry;
        }
      }
    }
    return localTempFile;
  }
}
