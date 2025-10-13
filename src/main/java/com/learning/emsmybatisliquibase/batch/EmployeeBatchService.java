package com.learning.emsmybatisliquibase.batch;

import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBatchService {

    private final EmployeeService employeeService;
    private static final int CHUNK_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0");

    static {
        DECIMAL_FORMAT.setMaximumFractionDigits(0);
    }

    public List<UUID> processEmployeeBatch(List<List<String>> rowData) {
        log.info("Starting batch processing for {} rows", rowData.size());
        
        List<UUID> employeeUuids = new ArrayList<>();
        List<AddEmployeeDto> batch = new ArrayList<>();
        int processedCount = 0;
        
        for (List<String> row : rowData) {
            AddEmployeeDto employee = parseEmployee(row);
            
            if (employee != null) {
                batch.add(employee);
                
                if (batch.size() == CHUNK_SIZE) {
                    employeeUuids.addAll(processBatch(batch));
                    processedCount += batch.size();
                    log.info("Processed {} employees", processedCount);
                    batch.clear();
                }
            }
        }
        
        if (!batch.isEmpty()) {
            employeeUuids.addAll(processBatch(batch));
            processedCount += batch.size();
        }
        
        log.info("Batch processing completed. Total: {} employees", processedCount);
        return employeeUuids;
    }
    
    private AddEmployeeDto parseEmployee(List<String> row) {
        if (row.size() != 14) {
            log.warn("Skipping invalid row with {} columns", row.size());
            return null;
        }
        
        try {
            return AddEmployeeDto.builder()
                    .firstName(row.get(0))
                    .lastName(row.get(1))
                    .email(row.get(2))
                    .gender(row.get(3).equals("M") ? Gender.MALE : Gender.FEMALE)
                    .dateOfBirth(parseDate(row.get(4)))
                    .phoneNumber(DECIMAL_FORMAT.format(Double.parseDouble(row.get(5))))
                    .joiningDate(parseDate(row.get(6)))
                    .leavingDate(parseDate(row.get(7)))
                    .departmentName(row.get(8).trim())
                    .isManager(row.get(9).trim())
                    .managerUuid(row.get(10).trim().isEmpty() ? null : UUID.fromString(row.get(10).trim()))
                    .jobTitle(row.get(11))
                    .password(row.get(12))
                    .confirmPassword(row.get(13))
                    .build();
        } catch (Exception e) {
            log.error("Error parsing employee: {}", e.getMessage());
            return null;
        }
    }
    
    private List<UUID> processBatch(List<AddEmployeeDto> batch) {
        List<UUID> uuids = new ArrayList<>();
        
        for (AddEmployeeDto employee : batch) {
            try {
                uuids.add(employeeService.add(employee).getUuid());
            } catch (Exception e) {
                log.error("Failed to add employee {}: {}", employee.getEmail(), e.getMessage());
            }
        }
        
        return uuids;
    }
    
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }
}
