package com.learning.emsmybatisliquibase.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payroll implements Serializable {

    private UUID uuid;

    private UUID employeeUuid;

    private UUID salaryPaidUuid;

    private Date date;

    private Instant totalAmount;
}
