package com.learning.emsmybatisliquibase.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.emsmybatisliquibase.entity.enums.JobTitleType;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Profile implements Serializable {

    private JobTitleType jobTitle;

    private ProfileStatus profileStatus;

    private UUID employeeUuid;

    private UUID departmentUuid;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updatedTime;
}
