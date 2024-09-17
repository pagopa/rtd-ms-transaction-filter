package it.gov.pagopa.rtd.transaction_filter.logger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
@Slf4j
class MaskingPatternLayoutTest {

  @Test
  void whenLogContainsSensibleDataThenAnonymizeIt(CapturedOutput output) {
    log.debug("Ocp-Apim-Subscription-Key: {}", "apikeytest");

    String anonymizedString = getAnonymizedStringFromOutput(output);

    assertThat(anonymizedString).isNotEmpty();
  }

  @Test
  void whenLogDoesNotContainsSensibleDataThenDoNothing(CapturedOutput output) {
    log.debug("Log example without sensible data");

    String anonymizedString = getAnonymizedStringFromOutput(output);

    assertThat(anonymizedString).isEmpty();
  }

  @Test
  void whenLogContainsSaltThenAnonymizeIt(CapturedOutput output) {
    String stringWithSalt = """
        [HpanRestConnector#getSalt] ---> GET https://api.dev.cstar.pagopa.it/rtd/payment-instrument-manager/v2/salt HTTP/1.1
        [HpanRestConnector#getSalt] Ocp-Apim-Subscription-Key: ciao
        [HpanRestConnector#getSalt] User-Agent: BatchService/2.2.3
        [HpanRestConnector#getSalt] ---> END HTTP (0-byte body)
        [HpanRestConnector#getSalt] <--- HTTP/1.1 200 OK (57ms)
        [HpanRestConnector#getSalt] connection: keep-alive
        [HpanRestConnector#getSalt] content-length: 7
        [HpanRestConnector#getSalt] date: Thu, 08 Sep 2022 10:35:15 GMT
        [HpanRestConnector#getSalt] request-context: appId=cid-v1:0236a5cc-b73b-454b-85c4-8728dd0d5963
        [HpanRestConnector#getSalt]
        [HpanRestConnector#getSalt] saltProva
        [HpanRestConnector#getSalt] <--- END HTTP (7-byte body)
        """;

    Arrays.stream(stringWithSalt.split("\n")).forEachOrdered(log::debug);

    String anonymizedString = getAnonymizedStringFromOutput(output);

    assertThat(anonymizedString).isNotEmpty();
  }

  @Test
  void whenLogLevelIsInfoThenAnonymizeSensibleData(CapturedOutput output) {
    log.info("Ocp-Apim-Subscription-Key: {}", "apikeytest");

    String anonymizedString = getAnonymizedStringFromOutput(output);

    assertThat(anonymizedString).isNotEmpty();
  }

  @Test
  void whenLogLevelIsWarningThenAnonymizeSensibleData(CapturedOutput output) {
    log.warn("Ocp-Apim-Subscription-Key: {}", "apikeytest");

    String anonymizedString = getAnonymizedStringFromOutput(output);

    assertThat(anonymizedString).isNotEmpty();
  }

  @Test
  void whenLogLevelIsErrorThenAnonymizeSensibleData(CapturedOutput output) {
    log.error("Ocp-Apim-Subscription-Key: {}", "apikeytest");

    String anonymizedString = getAnonymizedStringFromOutput(output);

    assertThat(anonymizedString).isNotEmpty();
  }

  private String getAnonymizedStringFromOutput(CapturedOutput output) {

    Pattern pattern = Pattern.compile("(\\*+)");
    Matcher matcher = pattern.matcher(output.getOut());

    if (matcher.find()) {
      return matcher.group();
    } else {
      return "";
    }
  }
}