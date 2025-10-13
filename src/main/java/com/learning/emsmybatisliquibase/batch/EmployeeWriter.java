package com.learning.emsmybatisliquibase.batch;

import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeWriter implements ItemWriter<AddEmployeeDto> {

    private final EmployeeService employeeService;
    private final List<UUID> createdUuids = new ArrayList<>();

    @Override
    public void write(Chunk<? extends AddEmployeeDto> chunk) {
        log.info("Writing batch of {} employees", chunk.size());
        
        for (AddEmployeeDto employee : chunk) {
            try {
                UUID uuid = employeeService.add(employee).getUuid();
                createdUuids.add(uuid);
            } catch (Exception e) {
                log.error("Failed to add employee {}: {}", employee.getEmail(), e.getMessage());
            }
        }
    }

    public List<UUID> getCreatedUuids() {
        return new ArrayList<>(createdUuids);
    }

    public void clearUuids() {
        createdUuids.clear();
    }
}
