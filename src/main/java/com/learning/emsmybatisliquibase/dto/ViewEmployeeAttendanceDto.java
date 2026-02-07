package com.learning.emsmybatisliquibase.dto;

import com.learning.emsmybatisliquibase.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ViewEmployeeAttendanceDto {
    private UUID employeeUuid;

    private String employeeFirstName;

    private String employeeLastName;

    private List<Attendance> submitted;

    private List<Attendance> approved;

    private List<Attendance> cancelled;
}
