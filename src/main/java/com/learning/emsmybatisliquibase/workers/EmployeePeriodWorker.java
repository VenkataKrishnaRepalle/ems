package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.EmployeePeriodDao;
import com.learning.emsmybatisliquibase.entity.Period;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.EmployeePeriodService;
import com.learning.emsmybatisliquibase.service.ProcessExecutionService;
import com.learning.emsmybatisliquibase.utils.UtilityService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeePeriodWorker {

    private final EmployeePeriodService employeePeriodService;

    private final ProcessExecutionService processExecutionService;

    private final EmployeeDao employeeDao;

    private final ObjectMapper objectMapper;

    private final EmployeePeriodDao employeePeriodDao;

    @JobWorker(type = "assign-employee-period")
    public void assignEmployeePeriod(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();

        if (!data.containsKey("employeeUuid")) {
            throw new InvalidInputException("INVALID_INPUT", "invalid input: employeeUuid");
        }

        UUID employeeUuid = objectMapper.convertValue(data.get("employeeUuid"), UUID.class);
        try {
            employeePeriodService.periodAssignment(List.of(employeeUuid));
            log.info("Employee Periods assigned for employee: {}", employeeUuid);
        } catch (Exception e) {
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("compensate-keycloak-employee-error")
                    .errorMessage("Employee assignment failed for employee: " + employeeUuid)
                    .send()
                    .join();

            processExecutionService.updateErrorDetails(job.getProcessInstanceKey(),
                    "assign-employee-period",
                    "EMPLOYEE_PERIOD_ASSIGNMENT_FAILED",
                    "Failed to assign employee period for employee: " + employeeUuid);
        }
    }

    @JobWorker(type = "employee-period-assign-bulk-preparation")
    public void prepareBulkPeriodAssignPreparation(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();

        var employeePeriodEligibleCount = employeeDao.employeesCount(List.of(ProfileStatus.ACTIVE));
        variables.put("has_more_colleagues", true);
        UtilityService.setBatchSizeAndDelay(employeePeriodEligibleCount, variables);

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }


    @JobWorker(type = "assign-employee-period-bulk")
    public void assignEmployeePeriodBulk(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();

        UtilityService.setBatchSizeAndDelay(employeeDao.employeesCount(List.of(ProfileStatus.ACTIVE)), variables);

        var limit = objectMapper.convertValue(variables.get("batchSize"), Integer.class);
        var period = objectMapper.convertValue(variables.get("period"), Period.class);
        List<UUID> employeeUuids = employeePeriodDao.getAllByEmployeeUuidsByPeriodId(period.getUuid(), limit);

        if (employeeUuids.isEmpty()) {
            variables.put("has_more_colleagues", false);

            variables.put("quarterType", ReviewType.Q1);
            variables.put("startDate", period.getStartTime());
            variables.put("waitingDate", period.getStartTime());
            variables.put("quarterStatus", ReviewTimelineStatus.STARTED);
            variables.put("has_more_colleagues_to_update_quarter", true);
        } else {
            employeePeriodService.periodAssignment(employeeUuids, period);
        }

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }

}
