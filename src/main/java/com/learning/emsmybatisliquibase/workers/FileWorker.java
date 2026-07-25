package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dao.EmployeeBatchOnboardingDao;
import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.service.EmployeeService;
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
public class FileWorker {

    private final EmployeeService employeeService;

    private final ObjectMapper objectMapper;

    private final EmployeeBatchOnboardingDao employeeBatchOnboardingDao;

    @JobWorker(type = "process-each-batch", autoComplete = true)
    public void processBatch(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();

        var importId = objectMapper.convertValue(data.get("import_id"), String.class);
        var batchSize = objectMapper.convertValue(data.get("batch_size"), Integer.class);
        var currentBatch = objectMapper.convertValue(data.get("current_batch"), Integer.class);

        var batches = employeeBatchOnboardingDao.get(new RequestQuery(Map.of(
                "importId", importId,
                "batchNo", currentBatch
        )));
        if (batches.size() != 1) {
            return;
        }

        var batch = batches.getFirst();
        for (AddEmployeeDto employee : batch.getEmployees()) {
            log.info("Processing employee {}", employee.getEmail());
            employeeService.add(employee);
        }

        employeeBatchOnboardingDao.updateStatus(batch.getId(), "COMPLETED");

        Map<String, Object> variables = Map.of(
                "import_id", importId,
                "batch_size", batchSize,
                "current_batch", currentBatch + 1
        );

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }
}