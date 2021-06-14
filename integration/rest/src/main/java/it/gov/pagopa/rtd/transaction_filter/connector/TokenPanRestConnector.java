package it.gov.pagopa.rtd.transaction_filter.connector;

import it.gov.pagopa.rtd.transaction_filter.connector.model.TokenPanDataModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

@FeignClient(name = "${rest-client.tkm.serviceCode}", url = "${rest-client.tkm.base-url}")
public interface TokenPanRestConnector {

    @GetMapping(value = "${rest-client.bin.list.url}")
    TokenPanDataModel getBinList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping
    ResponseEntity<Resource> getDownloadList(
            URI baseUri,
            @RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping(value = "${rest-client.token.list.url}")
    TokenPanDataModel getTokenList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

}
