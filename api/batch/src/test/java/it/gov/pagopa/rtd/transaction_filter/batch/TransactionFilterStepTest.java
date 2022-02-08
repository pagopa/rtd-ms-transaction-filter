package it.gov.pagopa.rtd.transaction_filter.batch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep.filterValidFilenames;

class TransactionFilterStepTest {

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

}