package it.gov.pagopa.rtd.transaction_filter.connector;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "${rest-client.tkm.serviceCode}", url = "${rest-client.tkm.base-url}")
public interface TokenPanRestConnector {

    @GetMapping(value = "${rest-client.bin.list.url}")
    ResponseEntity<Resource> getBinList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping(value = "${rest-client.bin.list.url}")
    ResponseEntity<Resource> getBinPartialList(@RequestHeader("Ocp-Apim-Subscription-Key") String token,
                                            @RequestParam("filePartId") String filePartId);

    @GetMapping(value = "${rest-client.token.list.url}")
    ResponseEntity<Resource> getTokenList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping(value = "${rest-client.token.list.url}")
    ResponseEntity<Resource> getPartialTokenList(@RequestHeader("Ocp-Apim-Subscription-Key") String token,
                                            @RequestParam("filePartId") String filePartId);

}
