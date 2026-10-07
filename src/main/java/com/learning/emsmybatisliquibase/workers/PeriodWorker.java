package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.entity.Period;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.service.PeriodService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class PeriodWorker {

    private final PeriodService periodService;

    private final ObjectMapper objectMapper;

    private final CamundaClient camundaClient;

    private static final String PERIOD = "period";

    @JobWorker(name = "create-period", autoComplete = true)
    public void createPeriod(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        var year = objectMapper.convertValue(variables.get("year"), Integer.class);

        var period = periodService.createPeriod(year);

        variables.put(PERIOD, period);
        variables.put("periodUuid", period.getUuid());
        variables.put("nextPeriodScheduledDate", period.getEndTime().minusDays(7));
        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }

    @JobWorker(name = "update-period-status", autoComplete = true)
    public void updatePeriod(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        var newStatus = objectMapper.convertValue(variables.get("status"), PeriodStatus.class);
        var period = objectMapper.convertValue(variables.get(PERIOD), Period.class);

        period.setStatus(newStatus);
        period = periodService.update(period);

        log.info("Updated period with id {} to status {}", period.getUuid(), newStatus);

        variables.put(PERIOD, period);

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }

    @JobWorker(name = "start-new-period-flow", autoComplete = true)
    public void callNextPeriodFlow(final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        var nextYear = objectMapper.convertValue(variables.get("nextYear"), Integer.class);

        try {
            camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId("period-flow")
                    .latestVersion()
                    .variables(Map.of("year", nextYear))
                    .send();
        } catch (Exception e) {
            throw new IntegrityException("FAILED_TO_CALL_NEXT_YEAR_PERIOD_PROCESS", "Failed to call next year period process");
        }

        log.info("New period initialized for year {}", nextYear);
    }
}
