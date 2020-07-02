package it.gov.pagopa.rtd.transaction_filter.batch.example;

import lombok.SneakyThrows;
import org.apache.http.HttpResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hpan")
public class HpanController {

    @SneakyThrows
    @GetMapping(value = "/list")
    public ResponseEntity<Resource> getList(HttpRequest request, HttpResponse response) {
        Resource file = new UrlResource("file:C:/dev/trxs/test.zip");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=test.zip");
        response.setHeader("checksum", "1234");
        return new ResponseEntity<Resource>(file, headers, HttpStatus.OK);
    }

}
