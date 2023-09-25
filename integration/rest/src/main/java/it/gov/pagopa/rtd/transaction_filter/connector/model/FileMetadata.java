package it.gov.pagopa.rtd.transaction_filter.connector.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class FileMetadata {

  @NotNull
  @NotBlank
  private String name;

  private Long size;

  @NotNull
  @NotBlank
  private String status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @NotNull
  private LocalDateTime transmissionDate;

}
