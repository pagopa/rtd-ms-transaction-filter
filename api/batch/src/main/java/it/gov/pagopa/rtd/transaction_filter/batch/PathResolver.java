package it.gov.pagopa.rtd.transaction_filter.batch;

import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@RequiredArgsConstructor
public class PathResolver {

  private final PathMatchingResourcePatternResolver resolver;

  /**
   * Resolve symlinks and retrieve all the Resources inside the target path with csv format.
   *
   * @return array of Resource
   * @throws IOException if the file does not exist
   */
  public Resource[] getCsvResources(String path) throws IOException {
    //resolve symlinks
    var targetPathWithPrefix = resolveSymlink(path);
    return resolver.getResources(targetPathWithPrefix + "/*.csv");
  }

  private String resolveSymlink(String symlink) throws IOException {
    var targetPath = Path.of(symlink.replace("file:", "")).toRealPath();
    return "file:" + targetPath;
  }

  public Resource[] getResources(String locationPattern) throws IOException {
    return resolver.getResources(locationPattern);
  }

}
