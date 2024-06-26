package it.gov.pagopa.rtd.transaction_filter.connector.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  @NotNull
  private LocalDateTime transmissionDate;

  private Map<String, Object> dataSummary = new LinkedHashMap<>();

  @JsonAnySetter
  void setDataSummary(String key, Object value) {
    dataSummary.put(key, value);
  }

}
