package com.learning.emsmybatisliquibase.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.hpsf.Decimal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Salary {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID employeeUuid;

    private BigDecimal total;

    private BigDecimal fixed;

    private BigDecimal variable;

    private BigDecimal joiningBonus;

    private BigDecimal basic;

    private BigDecimal flex;

    private BigDecimal hra;

    private BigDecimal pf;

    private UUID createdBy;

    private LocalDateTime createdTime;

    private UUID updatedBy;

    private LocalDateTime updatedTime;

}
