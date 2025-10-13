package com.learning.emsmybatisliquibase.batch;

import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
public class EmployeeReader implements ItemReader<AddEmployeeDto> {

    private final Iterator<List<String>> iterator;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0");

    static {
        DECIMAL_FORMAT.setMaximumFractionDigits(0);
    }

    public EmployeeReader(List<List<String>> rowData) {
        this.iterator = rowData.iterator();
    }

    @Override
    public AddEmployeeDto read() {
        while (iterator.hasNext()) {
            List<String> row = iterator.next();
            
            if (row.size() != 14) {
                log.warn("Skipping invalid row with {} columns", row.size());
                continue;
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
            }
        }
        
        return null;
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }
}
