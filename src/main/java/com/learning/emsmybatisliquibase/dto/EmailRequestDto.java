package com.learning.emsmybatisliquibase.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EmailRequestDto {

  private EmailRecipientDetailsDto sender;

  private List<EmailRecipientDetailsDto> to;

  private String subject;

  private String htmlContent;
}
