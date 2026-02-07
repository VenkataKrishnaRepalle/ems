package com.learning.emsmybatisliquibase.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.emsmybatisliquibase.entity.Period;
import com.learning.emsmybatisliquibase.entity.ReviewTimeline;
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
public class EmployeeCycleAndTimelineResponseDto {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> years;

    private UUID employeeId;

    private UUID employeeCycleId;

    private Period period;

    private ReviewTimeline Q1;

    private ReviewTimeline Q2;

    private ReviewTimeline Q3;

    private ReviewTimeline Q4;
}
