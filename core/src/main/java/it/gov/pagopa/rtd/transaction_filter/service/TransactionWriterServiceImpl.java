package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionWriterServiceImpl implements TransactionWriterService {

    private final HashMap<String, BufferedWriter> fileChannelMap;

    @SneakyThrows
    @Override
    public void openFileChannel(String filename) {

        if (!fileChannelMap.containsKey(filename)) {
            Path path = Paths.get(filename);
            fileChannelMap.put(filename,Files.newBufferedWriter(path,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND));
        } else {
            log.warn("channel for file {} already opened", filename);
        }

    }

    @SneakyThrows
    @Override
    public void write(String filename, String content) {
        fileChannelMap.get(filename).append(content);
    }

    @Override
    public void closeAll() {
        fileChannelMap.keySet().forEach(filename -> {
            try {
                fileChannelMap.get(filename).close();
            } catch (IOException e) {
               log.error(e.getMessage(),e);
            }
        });
    }
}
