package it.gov.pagopa.rtd.transaction_filter.batch;

import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@RequiredArgsConstructor
public class PathResolver {

  public static final String FILE_PREFIX = "file:";
  private final PathMatchingResourcePatternResolver resolver;

  /**
   * Resolve symlinks for <code>file:</code> path and retrieve all the Resources inside the target path with csv format.
   * Other path type e.g. <code>classpath:</code> won't resolve symlink.
   *
   * @return array of Resource
   * @throws IOException if the file does not exist
   */
  public Resource[] getCsvResources(String path) throws IOException {
    var targetPathWithPrefix = resolveSymlink(path);
    return resolver.getResources(targetPathWithPrefix + "/*.csv");
  }

  private String resolveSymlink(String symlink) throws IOException {

    if (symlink.startsWith(FILE_PREFIX)) {
      var targetPath = Path.of(symlink.replace(FILE_PREFIX, "")).toRealPath();
      return FILE_PREFIX + targetPath;
    }

    return symlink;
  }

  public Resource[] getResources(String locationPattern) throws IOException {
    return resolver.getResources(locationPattern);
  }

}
