package com.learning.emsmybatisliquibase.dto;

import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeAttendanceDto extends AttendanceDto {

    private UUID employeeUuid;

    private String firstName;

    private String lastName;

    private UUID attendanceUuid;
}
