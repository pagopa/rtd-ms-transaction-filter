package it.gov.pagopa.rtd.transaction_filter.connector;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "${rest-client.hpan.serviceCode}", url = "${rest-client.hpan.base-url}")
public interface HpanRestConnector {

    @GetMapping(value = "${rest-client.hpan.list.url}")
    ResponseEntity<Resource> getHpanList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping(value = "${rest-client.hpan.list.url}")
    ResponseEntity<Resource> getPartialList(@RequestHeader("Ocp-Apim-Subscription-Key") String token,
                                            @RequestParam("filePartId") String filePartId);

    @GetMapping(value = "${rest-client.par.list.url}")
    ResponseEntity<Resource> getParList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping(value = "${rest-client.par.list.url}")
    ResponseEntity<Resource> getPartialParList(@RequestHeader("Ocp-Apim-Subscription-Key") String token,
                                            @RequestParam("filePartId") String filePartId);

    @GetMapping(value = "${rest-client.hpan.salt.url}")
    String getSalt(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

}
