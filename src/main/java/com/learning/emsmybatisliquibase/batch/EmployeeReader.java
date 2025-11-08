package com.learning.emsmybatisliquibase.batch;

import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
            
            if (row.size() != 15) {
                log.warn("Skipping invalid row with {} columns", row.size());
                continue;
            }

            try {
                return AddEmployeeDto.builder()
                        .firstName(row.get(0))
                        .lastName(row.get(1))
                        .email(row.get(2))
                        .gender(getGender(row.get(3)))
                        .dateOfBirth(parseDate(row.get(4)))
                        .phoneNumber(formatPhoneNumber(row.get(5)))
                        .joiningDate(parseDate(row.get(6)))
                        .leavingDate(parseDate(row.get(7)))
                        .departmentName(row.get(8).trim())
                        .isManager(row.get(9).trim())
                        .managerUuid(row.get(10).trim().isEmpty() ? null : UUID.fromString(row.get(10).trim()))
                        .managerEmail(row.get(11).trim().isEmpty() ? null : row.get(11).trim())
                        .jobTitle(row.get(12))
                        .password(row.get(13).trim())
                        .confirmPassword(row.get(14).trim())
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

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        try {
            // Remove any non-digit characters and trim
            String cleanNumber = phoneNumber.trim().replaceAll("[^\\d.]", "");
            // Parse and format to remove any decimal part
            return DECIMAL_FORMAT.format(Double.parseDouble(cleanNumber));
        } catch (NumberFormatException e) {
            log.error("Error parsing phone number: {}", phoneNumber, e);
            return null;
        }
    }

    private Gender getGender(String value) {
        if("M".equalsIgnoreCase(value) || "male".equalsIgnoreCase(value)) {
            return Gender.MALE;
        } else if("F".equalsIgnoreCase(value) || "female".equalsIgnoreCase(value)) {
            return Gender.FEMALE;
        } else {
            return null;
        }
    }
}
