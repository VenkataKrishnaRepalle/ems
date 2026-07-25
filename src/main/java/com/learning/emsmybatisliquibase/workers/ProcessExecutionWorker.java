package com.learning.emsmybatisliquibase.workers;

import com.learning.emsmybatisliquibase.entity.camunda.ProcessExecutionStatus;
import com.learning.emsmybatisliquibase.service.ProcessExecutionService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProcessExecutionWorker {

    private final ProcessExecutionService processExecutionService;

    @JobWorker(type = "complete-process-execution")
    public void completeProcessExecution(final JobClient client, final ActivatedJob job) {
        processExecutionService.updateStatus(job.getProcessInstanceKey(), ProcessExecutionStatus.COMPLETED);
        client.newCompleteCommand(job.getKey()).send();
    }
}
