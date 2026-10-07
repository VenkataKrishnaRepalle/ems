package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.EmployeePeriodDao;
import com.learning.emsmybatisliquibase.dao.ReviewTimelineDao;
import com.learning.emsmybatisliquibase.entity.Period;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.service.ReviewTimelineService;
import com.learning.emsmybatisliquibase.utils.UtilityService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class TimelineWorker {

    private final ReviewTimelineService reviewTimelineService;

    private final ObjectMapper objectMapper;

    private final EmployeeDao employeeDao;

    @JobWorker(type = "prepare-timeline", autoComplete = true)
    public void prepareTimeline(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        log.info("prepare timeline {}", variables);

        var totalEmpCount = employeeDao.employeesCount(List.of(ProfileStatus.ACTIVE));
        UtilityService.setBatchSizeAndDelay(totalEmpCount, variables);

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }

    @JobWorker(type = "update-quarter-status", autoComplete = true)
    public void updateQuarterStatus(final JobClient client, final ActivatedJob job) throws DataIntegrityViolationException {
        var variables = job.getVariablesAsMap();

        var period = objectMapper.convertValue(variables.get("period"), Period.class);
        var timelineStatus = objectMapper.convertValue(variables.get("reviewStatus"), ReviewTimelineStatus.class);
        var quarterType = objectMapper.convertValue(variables.get("reviewType"), ReviewType.class);

        int rows = reviewTimelineService.update(period.getUuid(), quarterType, timelineStatus);
        variables.put("rows", rows);

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }
}
