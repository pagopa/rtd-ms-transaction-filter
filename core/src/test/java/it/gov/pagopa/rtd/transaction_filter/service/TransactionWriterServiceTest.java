package it.gov.pagopa.rtd.transaction_filter.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;

public class TransactionWriterServiceTest {

    private final static String inputTrxFileNotSeen = "CSTAR.99999.TRNLOG.20220204.094652.001.csv";
    private final static String inputTrxFileSeen = "CSTAR.99999.TRNLOG.20220504.141151.001.csv";
    private final static String filePathPrefix = "/workdir/logs/20220505153114123_Rtd__ErrorRecords_";

    private TransactionWriterService transactionWriterService;

    @Before
    public void setUp() {
        HashMap<String, BufferedWriter> fileChannelMapMock = new HashMap<>();
        fileChannelMapMock.put(filePathPrefix + "CSTAR.99999.TRNLOG.20220505.132817.001.csv", null);
        fileChannelMapMock.put(filePathPrefix + "CSTAR.99999.TRNLOG.20220504.141151.001.csv", null);
        fileChannelMapMock.put(filePathPrefix + "CSTAR.99999.TRNLOG.20220503.132022.001.csv", null);
        transactionWriterService = new TransactionWriterServiceImpl(fileChannelMapMock, new TreeSet<>());
    }

    @Test
    public void shouldReturnFalseWhenFileChannelDoesNotExist() {
        assertFalse(transactionWriterService.existFileChannelForFilename(inputTrxFileNotSeen));
    }

    @Test
    public void shouldReturnTrueWhenFileChannelExists() {
        assertTrue(transactionWriterService.existFileChannelForFilename(inputTrxFileSeen));
    }

}