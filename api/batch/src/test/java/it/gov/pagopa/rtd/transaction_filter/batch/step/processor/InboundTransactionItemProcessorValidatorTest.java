package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolationException;


public class InboundTransactionItemProcessorValidatorTest {

    private final String STRING_LEN_20 = "12345678901234567890";
    private final String STRING_LEN_21 = STRING_LEN_20 + "1";
    private final String STRING_LEN_255 = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
    private final String STRING_LEN_256 = STRING_LEN_255 + "6";

    public InboundTransactionItemProcessorValidatorTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private HpanStoreService hpanStoreServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(hpanStoreServiceMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_21})
    public void processTransactionWithInvalidAcquirerCodeThrowsException(String acquirerCode) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAcquirerCode(acquirerCode);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71", "27cd4dd11ea5859", STRING_LEN_20})
    public void processTransactionWithValidAcquirerCode(String acquirerCode) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAcquirerCode(acquirerCode);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "010", "AA"})
    public void processTransactionWithInvalidOperationTypeThrowsException(String operationType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setOperationType(operationType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "01", "02", "03", "04"})
    public void processTransactionWithValidOperationType(String operationType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setOperationType(operationType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "010", "AA"})
    public void processTransactionWithInvalidCircuitTypeThrowsException(String circuitType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCircuitType(circuitType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10"})
    public void processTransactionWithValidCircuitType(String circuitType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCircuitType(circuitType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    public void processTransactionWithInvalidPanThrowsException(String pan) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPan(pan);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    public void processTransactionWithValidPan(String pan) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPan(pan);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    // TODO: datetime

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    public void processTransactionWithInvalidIdTrxAcquirerThrowsException(String idTrxAcquirer) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxAcquirer(idTrxAcquirer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    public void processTransactionWithValidIdTrxAcquirer(String idTrxAcquirer) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxAcquirer(idTrxAcquirer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058"})
    public void processTransactionWithValidIdTrxIssuer(String idTrxIssuer) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxIssuer(idTrxIssuer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058"})
    public void processTransactionWithValidCorrelationId(String correlationId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCorrelationId(correlationId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(longs = {1000, 200000})
    public void processTransactionWithValidAmount(Long amount) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmount(amount);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234"})
    public void processTransactionWithInvalidAmountCurrencyThrowsException(String amountCurrency) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmountCurrency(amountCurrency);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"978", "12", ""})
    public void processTransactionWithValidAmountCurrency(String amountCurrency) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmountCurrency(amountCurrency);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    public void processTransactionWithInvalidAcquirerIdThrowsException(String acquirerId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAcquirerId(acquirerId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058"})
    public void processTransactionWithValidAcquirerId(String acquirerId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAcquirerId(acquirerId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    public void processTransactionWithInvalidMerchantIdThrowsException(String merchantId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMerchantId(merchantId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058"})
    public void processTransactionWithValidMerchantId(String merchantId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMerchantId(merchantId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    public void processTransactionWithInvalidTerminalIdThrowsException(String terminalId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setTerminalId(terminalId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058"})
    public void processTransactionWithValidTerminalId(String terminalId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setTerminalId(terminalId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "12345A", "1234567"})
    public void processTransactionWithInvalidBinThrowsException(String bin) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setBin(bin);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "12345678"})
    public void processTransactionWithValidBin(String bin) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setBin(bin);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "123456"})
    public void processTransactionWithInvalidMccThrowsException(String mcc) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMcc(mcc);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "3000", "203"})
    public void processTransactionWithValidMcc(String mcc) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMcc(mcc);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "123456789012345678901234567890123456789012345678901"})
    public void processTransactionWithInvalidVatThrowsException(String vat) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setVat(vat);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15376371009", "DE256610065"})
    public void processTransactionWithValidVat(String vat) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setVat(vat);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "010", "AA"})
    public void processTransactionWithInvalidPosTypeThrowsException(String posType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPosType(posType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "01"})
    public void processTransactionWithValidPosType(String posType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPosType(posType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    @ParameterizedTest
    @ValueSource(strings = {STRING_LEN_256})
    public void processTransactionWithInvalidParThrowsException(String par) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPar(par);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a70352ebab7fba1d20d8c667c37495a458d4", "0a3eb9af89ef1d6735e0489ed7d09f84a76fa2eb8e332a4e0e1d37ed7", STRING_LEN_255})
    public void processTransactionWithValidPar(String par) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPar(par);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        processor.process(transaction);
    }

    private InboundTransaction fakeInboundTransaction() {
        return InboundTransaction.builder()
                .acquirerCode("001")
                .operationType("00")
                .circuitType("00")
                .pan("pan123")
                .trxDate("2020-04-09T16:22:45.304+00:00")
                .idTrxAcquirer("1")
                .idTrxIssuer("0")
                .correlationId("1")
                .amount(1000L)
                .amountCurrency("833")
                .acquirerId("0")
                .merchantId("0")
                .terminalId("0")
                .bin("000001")
                .mcc("813")
                .vat("12345678901")
                .posType("00")
                .build();
    }
}