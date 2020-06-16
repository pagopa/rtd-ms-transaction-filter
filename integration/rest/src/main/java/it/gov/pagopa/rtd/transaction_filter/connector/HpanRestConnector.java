package it.gov.pagopa.rtd.transaction_filter.connector;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "${rest-client.hpan.serviceCode}", url = "${rest-client.hpan.base-url}")
public interface HpanRestConnector {

    @GetMapping(value = "${rest-client.hpan.list.url}")
    Resource getList();

    @GetMapping(value = "${rest-client.hpan.salt.url}")
    String getSalt();

}
