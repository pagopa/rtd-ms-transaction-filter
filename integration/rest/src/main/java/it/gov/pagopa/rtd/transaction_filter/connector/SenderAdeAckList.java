package it.gov.pagopa.rtd.transaction_filter.connector;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SenderAdeAckList {

  List<String> fileNameList = new ArrayList<>();
}