package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Executors;

/**
* Implementation of {@link HpanStoreService}
 */

@Service
@RequiredArgsConstructor
@Slf4j
class HpanStoreServiceImpl implements HpanStoreService {

    private final HashMap<String, BufferedWriter> bufferedWriterHashMap;
    private final HashMap<String, Path> hpanFiles;
    private Integer fileMargin = 1;
    private String salt = "";

    private static final Set<String> lockedIds = new HashSet<>();

    private void lock(String id) throws InterruptedException {
        synchronized (lockedIds) {
            while (!lockedIds.add(id)) {
                lockedIds.wait();
            }
        }
    }

    private void unlock(String id) {
        synchronized (lockedIds) {
            lockedIds.remove(id);
            lockedIds.notifyAll();
        }
    }

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
    public void store(String hpan) {
        String filePrefix = hpan.substring(0,Math.min(fileMargin,hpan.length()));
        BufferedWriter bufferedWriter = getBufferedWriter(filePrefix);
        assert bufferedWriter != null;
        bufferedWriter.write(hpan.concat("\n"));
    }

    @SneakyThrows
    @Override
    public Boolean hasHpan(String hpan, long lineNumber) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(hpanFiles.get(
                hpan.substring(0,Math.min(fileMargin,hpan.length()))))) {
            String fileHpan;
            while ((fileHpan = bufferedReader.readLine()) != null) {
                if (hpan.equals(fileHpan)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SneakyThrows
    @Override
    public void clearAll() {
        for (BufferedWriter bufferedWriter : bufferedWriterHashMap.values()) {
            bufferedWriter.close();
        }
        hpanFiles.clear();
        this.salt = "";
    }

    @Override
    public void setFileMargin(int fileMargin) {
        this.fileMargin = fileMargin;
    }

    @SneakyThrows
    @Override
    public void closeAllWriters() {
       for (BufferedWriter bufferedWriter : bufferedWriterHashMap.values()) {
           bufferedWriter.close();
       }
    }

    @SneakyThrows
    private BufferedWriter getBufferedWriter(String filePrefix) {
        BufferedWriter bufferedWriter;
        try {
            lock(filePrefix);
            if (hpanFiles.containsKey(filePrefix)) {
                bufferedWriter = bufferedWriterHashMap.get(filePrefix);
                if (bufferedWriter == null) {
                    openBufferedWriter(filePrefix);
                }
            } else {
                bufferedWriter = openBufferedWriter(filePrefix);
            }
        } finally {
            unlock(filePrefix);
        }
        return bufferedWriter;
    }

    @SneakyThrows
    private BufferedWriter openBufferedWriter(String filePrefix) {
        String fileName = filePrefix.replaceAll("\\W", "");
        Path localFile = Files.createTempFile("tempFile".concat(fileName), ".csv");
        hpanFiles.put(filePrefix, localFile);
        BufferedWriter bufferedWriter = Files.newBufferedWriter(localFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        bufferedWriterHashMap.put(filePrefix, bufferedWriter);
        return bufferedWriter;
    }


}
