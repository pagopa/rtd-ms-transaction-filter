package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
* Implementation of {@link HpanStoreService}
 */

@Service
@RequiredArgsConstructor
@Slf4j
class HpanStoreServiceImpl implements HpanStoreService {

    private final List<BufferedWriter> bufferedWriterList;
    private final TreeSet<String> hpanSet;
    private String workingHpanDirectory;
    private Long numberPerFile;
    private Long currentNumberOfData = 0L;
    private String salt = "";

    @Override
    public void storeSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public String getSalt() {
        return this.salt;
    }

    @SneakyThrows
    @Override
    public void write(String hpan) {
        BufferedWriter bufferedWriter = getBufferedWriter();
        assert bufferedWriter != null;
        bufferedWriter.write(hpan.concat("\n"));
    }

    @Override
    public synchronized void store(String hpan) {
        hpanSet.add(hpan);
    }

    @Override
    public Boolean hasHpan(String hpan) {
        return hpanSet.contains(hpan);
    }

    @SneakyThrows
    @Override
    public void clearAll() {
        hpanSet.clear();
        for (BufferedWriter bufferedWriter : bufferedWriterList) {
            bufferedWriter.close();
        }
    }

    @SneakyThrows
    @Override
    public void clearStoreSet() {
        hpanSet.clear();
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
            Resource resource = resolver.getResource(workingHpanDirectory);
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
    public void setWorkingHpanDirectory(String workingHpanDirectory) {
        this.workingHpanDirectory = workingHpanDirectory;
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
