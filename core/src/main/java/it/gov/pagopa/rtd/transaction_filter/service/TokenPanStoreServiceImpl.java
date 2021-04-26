package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.TreeSet;

/**
* Implementation of {@link HpanStoreService}
 */

@Service
@RequiredArgsConstructor
@Slf4j
class TokenPanStoreServiceImpl implements TokenPanStoreService {

    private final List<BufferedWriter> bufferedWriterList;
    private final TreeSet<String> tokenPanSet;
    private String workingTokenPanDirectory;
    private Long numberPerFile;
    private Long currentNumberOfData = 0L;

    @SneakyThrows
    @Override
    public void write(String hpan) {
        BufferedWriter bufferedWriter = getBufferedWriter();
        assert bufferedWriter != null;
        bufferedWriter.write(hpan.concat("\n"));
    }

    @Override
    public synchronized void store(String tokenPan) {
        tokenPanSet.add(tokenPan);
    }

    @Override
    public Boolean hasTokenPAN(String tokenPan) {
        return tokenPanSet.contains(tokenPan);
    }

    @SneakyThrows
    @Override
    public void clearAll() {
        tokenPanSet.clear();
        for (BufferedWriter bufferedWriter : bufferedWriterList) {
            bufferedWriter.close();
        }
    }

    @SneakyThrows
    @Override
    public void clearStoreSet() {
        tokenPanSet.clear();
    }

    @SneakyThrows
    @Override
    public void closeAllWriters() {
       for (BufferedWriter bufferedWriter : bufferedWriterList) {
           bufferedWriter.close();
       }
    }

    @SneakyThrows
    private BufferedWriter getBufferedWriter() {
        int data = bufferedWriterList.size();
        Long currentData =  getCurrentNumberOfData();
        if (Math.multiplyExact(data,numberPerFile) > currentData) {
            return bufferedWriterList.get(bufferedWriterList.size()-1);
        } else {
            synchronized (bufferedWriterList) {
                data = bufferedWriterList.size();
                if (Math.multiplyExact(data,numberPerFile) > currentData) {
                    return bufferedWriterList.get(bufferedWriterList.size()-1);
                } else {
                    return openBufferedWriter(bufferedWriterList.size());
                }
            }
        }
    }

    @SneakyThrows
    private synchronized BufferedWriter openBufferedWriter(int pageNumber) {
        BufferedWriter bufferedWriter;
        if (pageNumber == bufferedWriterList.size()-1) {
            bufferedWriter = bufferedWriterList.get(pageNumber-1);
        } else {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(workingTokenPanDirectory);
            bufferedWriter = Files.newBufferedWriter(
                    Paths.get(resource.getFile().getAbsolutePath().concat("/".concat
                            ("temp".concat(String.valueOf(pageNumber)).concat(".csv")))),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            bufferedWriterList.add(bufferedWriter);
        }
        return bufferedWriter;
    }

    private synchronized Long getCurrentNumberOfData() {
        currentNumberOfData = currentNumberOfData + 1;
        return currentNumberOfData;
    }

    @Override
    public void setWorkingTokenPANDirectory(String workingTokenPanDirectory) {
        this.workingTokenPanDirectory = workingTokenPanDirectory;
    }

    @Override
    public void setNumberPerFile(Long numberPerFile) {
        this.numberPerFile = numberPerFile;
    }

    @Override
    public void setCurrentNumberOfData(Long currentNumberOfData) {
        this.currentNumberOfData = currentNumberOfData;
    }

}
