package it.gov.pagopa.rtd.transaction_filter.connector.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenPanDataModel {

    private List<String> fileLinks;
    private Integer numberOfFiles;
    private String availableUntil;
    private String generationDate;

}
