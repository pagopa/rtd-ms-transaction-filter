package it.gov.pagopa.rtd.transaction_filter.connector;

import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "${rest-client.hpan.serviceCode}", url = "${rest-client.hpan.base-url}")
public interface HpanRestConnector {

    @GetMapping(value = "${rest-client.hpan.list.url}")
    ResponseEntity<Resource> getList(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @GetMapping(value = "${rest-client.hpan.salt.url}")
    String getSalt(@RequestHeader("Ocp-Apim-Subscription-Key") String token);

    @PostMapping(value = "${rest-client.hpan.adesas.url}")
    // @Param placeholder is there only to force Feign to add a 'Content-length' header to the request
    SasResponse postAdeSas(@RequestHeader("Ocp-Apim-Subscription-Key") String token, @Param("placeholder") String placeholder);

    @PostMapping(value = "${rest-client.hpan.cstarsas.url}")
    // @Param placeholder is there only to force Feign to add a 'Content-length' header to the request
    SasResponse postCstarSas(@RequestHeader("Ocp-Apim-Subscription-Key") String token, @Param("placeholder") String placeholder);

}
