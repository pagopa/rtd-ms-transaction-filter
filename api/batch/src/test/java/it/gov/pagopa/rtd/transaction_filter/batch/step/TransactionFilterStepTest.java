package it.gov.pagopa.rtd.transaction_filter.batch.step;

import static it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep.filterResourcesByFilename;
import static it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep.filterValidFilenames;
import static org.assertj.core.api.Assertions.assertThat;

import it.gov.pagopa.rtd.transaction_filter.batch.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreServiceImpl;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class TransactionFilterStepTest {

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    static Stream<Arguments> arrayProvider() {
        return Stream.of(
                Arguments.of((Object) new String[][]{{}, {}}),
                Arguments.of((Object) new String[][]{{"trx.csv"}, {}}),
                Arguments.of((Object) new String[][]{{"CSTR.99999.TRNLOG.20220204.094652.001.csv"}, {}}),
                Arguments.of((Object) new String[][]{{"CSTAR.99999.TRNLOG.2022FEB04.094652.001.csv"}, {}}),
                Arguments.of((Object) new String[][]{{"CSTAR.99999.TRNLOG.20220204.094652.001.csv"}, {"CSTAR.99999.TRNLOG.20220204.094652.001.csv"}}),
                Arguments.of((Object) new String[][]{{"CSTAR.99999.TRNLOG.20220204.094652.001.csv", "trx.csv"}, {"CSTAR.99999.TRNLOG.20220204.094652.001.csv"}}),
                Arguments.of((Object) new String[][]{{"CSTAR.99999.TRNLOG.20220204.094652.001.csv", "CSTAR.99999.TRNLOG.20220204.094652.002.csv"}, {"CSTAR.99999.TRNLOG.20220204.094652.001.csv", "CSTAR.99999.TRNLOG.20220204.094652.002.csv"}})
        );
    }

    static Stream<Arguments> filenamesToMatch() {
        return Stream.of(
            Arguments.of((Object) new String[][]{{}, {}}),
            Arguments.of((Object) new String[][]{{"a.csv", "b.csv", "c.sv"}, {"b.csv"}}),
            Arguments.of((Object) new String[][]{{"a.csv", "c.csv"}, {}}),
            Arguments.of((Object) new String[][]{{"a.csv", "b.csv", "b.csv", "c.csv"}, {"b.csv"}})
        );
    }

    @ParameterizedTest
    @MethodSource("arrayProvider")
    void inputFilesMustAdhereToNamingConvention(String[][] args) {
        List<Resource> resources = new ArrayList<>();
        for (String filename : args[0]) {
            Resource mockedResource = Mockito.mock(Resource.class);
            BDDMockito.doReturn(filename).when(mockedResource).getFilename();
            resources.add(mockedResource);
        }
        Resource[] resourceArray = new Resource[resources.size()];
        resourceArray = resources.toArray(resourceArray);

        resourceArray = filterValidFilenames(resourceArray);

        Set<String> outputFilenames = Arrays.stream(resourceArray).map(Resource::getFilename).collect(Collectors.toSet());
        Set<String> expectedFilenames = Arrays.stream(args[1]).collect(Collectors.toSet());
        Assertions.assertEquals(expectedFilenames, outputFilenames);
    }

    @ParameterizedTest
    @MethodSource("filenamesToMatch")
    void inputFilesShouldBeFilteredByFilename(String[][] args) {
        String filenameToFilter = "b.csv";

        List<Resource> resources = new ArrayList<>();
        for (String filename : args[0]) {
            Resource mockedResource = Mockito.mock(Resource.class);
            BDDMockito.doReturn(filename).when(mockedResource).getFilename();
            resources.add(mockedResource);
        }
        Resource[] resourceArray = new Resource[resources.size()];
        resourceArray = resources.toArray(resourceArray);

        resourceArray = filterResourcesByFilename(resourceArray, filenameToFilter);

        Set<String> outputFilenames = Arrays.stream(resourceArray).map(Resource::getFilename).collect(Collectors.toSet());
        Set<String> expectedFilenames = Arrays.stream(args[1]).collect(Collectors.toSet());
        Assertions.assertEquals(expectedFilenames, outputFilenames);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/test/absolute/path/CSTAR.11111.20220925.101112.001.csv",
        "test/relative/path/CSTAR.11111.20220925.101112.001.csv"
    })
    void whenAggregatesAreSplittedThenNamingConventionIsAsExpected(String filePath) {
        TransactionFilterStep transactionFilterStep = new TransactionFilterStep(null, null);

        String firstChunkName = transactionFilterStep.getAdeOutputFileNameChunked(filePath, 0);
        String secondChunkName = transactionFilterStep.getAdeOutputFileNameChunked(filePath, 1);

        assertThat(firstChunkName).isEqualTo("ADE.11111.20220925.101112.001.00.csv");
        assertThat(secondChunkName).isEqualTo("ADE.11111.20220925.101112.001.01.csv");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/test/absolute/path/CSTAR.11111.20220925.101112.001.csv",
        "test/relative/path/CSTAR.11111.20220925.101112.001.csv"
    })
    void whenAggregatesAreSplittedThenGetItemWriterCorrect(String filePath) {
        TransactionFilterStep transactionFilterStep = new TransactionFilterStep(null, null);
        transactionFilterStep.setAdeSplitThreshold(100);
        transactionFilterStep.setOutputDirectoryPath("classpath:/test-encrypt");
        transactionFilterStep.setInputFileChecksumEnabled(false);
        transactionFilterStep.setApplyEncrypt(false);
        StoreService storeService = new StoreServiceImpl(null);
        storeService.storeKey("pagopa", "prova");

        ItemWriter<AdeTransactionsAggregate> itemWriter = transactionFilterStep.getAdeItemWriter(storeService);

        assertThat(itemWriter).isNotNull();
    }
}