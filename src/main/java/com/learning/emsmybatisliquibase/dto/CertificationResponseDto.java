package com.learning.emsmybatisliquibase.dto;

import com.learning.emsmybatisliquibase.entity.CertificationCategory;
import com.learning.emsmybatisliquibase.entity.enums.CertificationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CertificationResponseDto {

    private UUID uuid;

    private UUID employeeUuid;

    private CertificationCategory certificationCategory;

    private String name;

    private String skillSets;

    private CertificationLevel level;

    private Date certifiedDate;

    private Date expiryDate;

    private LocalDateTime creationTime;

    private LocalDateTime updatedTime;
}
