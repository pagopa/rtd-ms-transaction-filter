package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolationException;


public class InboundTransactionItemProcessorValidatorTest {

    private static final String FAKE_ENROLLED_PAN = "pan123";
    private static final String FAKE_SALT = "testSalt";
    private static final String STRING_LEN_20 = "12345678901234567890";
    private static final String STRING_LEN_21 = STRING_LEN_20 + "1";
    private static final String STRING_LEN_50 = "12345678901234567890123456789012345678901234567890";
    private static final String STRING_LEN_51 = STRING_LEN_50 + "1";
    private static final String STRING_LEN_64 = "1234567890123456789012345678901234567890123456789012345678901234";
    private static final String STRING_LEN_65 = STRING_LEN_64 + 1;
    private static final String STRING_LEN_255 = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
    private static final String STRING_LEN_256 = STRING_LEN_255 + "6";

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
    private StoreService storeServiceMock;

    @BeforeEach
    public void setUp() {
        Mockito.reset(storeServiceMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_21})
    void processTransactionWithInvalidSenderCodeThrowsException(String senderCode) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setSenderCode(senderCode);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71", "27cd4dd11ea5859", STRING_LEN_20})
    void processTransactionWithValidSenderCode(String senderCode) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setSenderCode(senderCode);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(senderCode, outbound.getSenderCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "010", "AA", "02", "03", "04"})
    void processTransactionWithInvalidOperationTypeThrowsException(String operationType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setOperationType(operationType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "01"})
    void processTransactionWithValidOperationType(String operationType) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setOperationType(operationType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(operationType, outbound.getOperationType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "010", "AA"})
    void processTransactionWithInvalidCircuitTypeThrowsException(String circuitType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCircuitType(circuitType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10"})
    void processTransactionWithValidCircuitType(String circuitType) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCircuitType(circuitType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(circuitType, outbound.getCircuitType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_65})
    void processTransactionWithInvalidPanThrowsException(String pan) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPan(pan);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_64})
    void processTransactionWithValidPan(String pan) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(pan);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPan(pan);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        String hashedPan = DigestUtils.sha256Hex(pan + FAKE_SALT);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(hashedPan, outbound.getPan());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    void processTransactionWithInvalidTrxDateThrowsException(String trxDate) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setTrxDate(trxDate);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2020-08-06T12:19:16.000+00:00", "2020-08-07T16:03:53.000+00:00"})
    void processTransactionWithValidTrxDate(String trxDate) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setTrxDate(trxDate);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(trxDate, outbound.getTrxDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_256})
    void processTransactionWithInvalidIdTrxAcquirerThrowsException(String idTrxAcquirer) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxAcquirer(idTrxAcquirer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    void processTransactionWithValidIdTrxAcquirer(String idTrxAcquirer) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxAcquirer(idTrxAcquirer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(idTrxAcquirer, outbound.getIdTrxAcquirer());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_256})
    void processTransactionWithInvalidIdTrxIssuerThrowsException(String idTrxIssuer) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxIssuer(idTrxIssuer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    void processTransactionWithValidIdTrxIssuer(String idTrxIssuer) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setIdTrxIssuer(idTrxIssuer);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(idTrxIssuer, outbound.getIdTrxIssuer());
    }

    @ParameterizedTest
    @ValueSource(strings = {STRING_LEN_256})
    void processTransactionWithInvalidCorrelationIdThrowsException(String correlationId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCorrelationId(correlationId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    void processTransactionWithValidCorrelationId(String correlationId) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setCorrelationId(correlationId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1000, 200000, Integer.MAX_VALUE + 1L, 3000000000L, 99999999999L})
    void processTransactionWithValidAmount(Long amount) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmount(amount);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(amount, outbound.getAmount());
    }

    @ParameterizedTest
    @ValueSource(longs = {-100L, 100000000000L})
    void processTransactionWithInvalidAmount(Long amount) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmount(amount);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "412", "15", ""})
    void processTransactionWithInvalidAmountCurrencyThrowsException(String amountCurrency) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmountCurrency(amountCurrency);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"978"})
    void processTransactionWithValidAmountCurrency(String amountCurrency) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAmountCurrency(amountCurrency);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(amountCurrency, outbound.getAmountCurrency());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_256})
    void processTransactionWithInvalidAcquirerIdThrowsException(String acquirerId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAcquirerId(acquirerId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    void processTransactionWithValidAcquirerId(String acquirerId) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setAcquirerId(acquirerId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(acquirerId, outbound.getAcquirerId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_256})
    void processTransactionWithInvalidMerchantIdThrowsException(String merchantId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMerchantId(merchantId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    void processTransactionWithValidMerchantId(String merchantId) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMerchantId(merchantId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(merchantId, outbound.getMerchantId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", STRING_LEN_256})
    void processTransactionWithInvalidTerminalIdThrowsException(String terminalId) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setTerminalId(terminalId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fae71031e132166", "27cd4dd11ea58592f41aaecaa1d31bcd1538ea29c068141daf77744893a2a058", STRING_LEN_255})
    void processTransactionWithValidTerminalId(String terminalId) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setTerminalId(terminalId);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(terminalId, outbound.getTerminalId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "12345A", "1234567"})
    void processTransactionWithInvalidBinThrowsException(String bin) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setBin(bin);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "12345678"})
    void processTransactionWithValidBin(String bin) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setBin(bin);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(bin, outbound.getBin());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "123456"})
    void processTransactionWithInvalidMccThrowsException(String mcc) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMcc(mcc);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "3000", "203"})
    void processTransactionWithValidMcc(String mcc) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setMcc(mcc);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(mcc, outbound.getMcc());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "    ", STRING_LEN_51})
    void processTransactionWithInvalidFiscalCodeThrowsException(String fiscalCode) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setFiscalCode(fiscalCode);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15376371009", "DE256610065", STRING_LEN_50})
    void processTransactionWithValidFiscalCode(String fiscalCode) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setFiscalCode(fiscalCode);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(fiscalCode, outbound.getFiscalCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {STRING_LEN_51})
    void processTransactionWithInvalidVatThrowsException(String vat) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setVat(vat);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "15376371009", "DE256610065", STRING_LEN_50})
    void processTransactionWithValidVat(String vat) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setVat(vat);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(vat, outbound.getVat());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "02", "08", "010", "AA"})
    void processTransactionWithInvalidPosTypeThrowsException(String posType) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPosType(posType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "01"})
    void processTransactionWithValidPosType(String posType) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPosType(posType);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(posType, outbound.getPosType());
    }

    @ParameterizedTest
    @ValueSource(strings = {STRING_LEN_256})
    void processTransactionWithInvalidParThrowsException(String par) {
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPar(par);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        Assertions.assertThrows(ConstraintViolationException.class, () -> processor.process(transaction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a70352ebab7fba1d20d8c667c37495a458d4", "0a3eb9af89ef1d6735e0489ed7d09f84a76fa2eb8e332a4e0e1d37ed7", STRING_LEN_255})
    void processTransactionWithValidPar(String par) {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransaction transaction = fakeInboundTransaction();
        transaction.setPar(par);
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outbound = processor.process(transaction);
        Assertions.assertNotNull(outbound);
        Assertions.assertEquals(par, outbound.getPar());
    }

    private InboundTransaction fakeInboundTransaction() {
        return InboundTransaction.builder()
                .senderCode("001")
                .operationType("00")
                .circuitType("00")
                .pan(FAKE_ENROLLED_PAN)
                .trxDate("2020-04-09T16:22:45.304+00:00")
                .idTrxAcquirer("1")
                .idTrxIssuer("0")
                .correlationId("1")
                .amount(1000L)
                .amountCurrency("978")
                .acquirerId("0")
                .merchantId("0")
                .terminalId("0")
                .bin("000001")
                .mcc("813")
                .fiscalCode("fc123456")
                .vat("12345678901")
                .posType("00")
                .build();
    }
}