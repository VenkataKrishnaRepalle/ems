package com.learning.emsmybatisliquibase.dto;

import com.learning.emsmybatisliquibase.entity.enums.AttendanceStatus;
import com.learning.emsmybatisliquibase.entity.enums.AttendanceType;
import com.learning.emsmybatisliquibase.entity.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDto {

    private WorkMode workMode;

    private AttendanceType type;

    private AttendanceStatus status;

    private LocalDate date;
}
