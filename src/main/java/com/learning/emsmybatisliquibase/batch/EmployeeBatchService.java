package com.learning.emsmybatisliquibase.batch;

import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBatchService {

    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EmployeeWriter employeeWriter;
    
    private static final int CHUNK_SIZE = 10;

    public List<UUID> processEmployeeBatch(List<List<String>> rowData) throws Exception {
        log.info("Starting Spring Batch processing for {} rows", rowData.size());
        
        employeeWriter.clearUuids();
        
        Step step = new StepBuilder("employeeStep", jobRepository)
                .<AddEmployeeDto, AddEmployeeDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(new EmployeeReader(rowData))
                .writer(employeeWriter)
                .build();
        
        Job job = new JobBuilder("employeeJob", jobRepository)
                .start(step)
                .build();
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution execution = jobLauncher.run(job, jobParameters);
        
        log.info("Batch job completed with status: {}", execution.getStatus());
        log.info("Total employees created: {}", employeeWriter.getCreatedUuids().size());
        
        return employeeWriter.getCreatedUuids();
    }
}
