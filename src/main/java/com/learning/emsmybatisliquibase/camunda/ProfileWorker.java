package com.learning.emsmybatisliquibase.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dto.AddDepartmentDto;
import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.entity.Department;
import com.learning.emsmybatisliquibase.entity.Profile;
import com.learning.emsmybatisliquibase.entity.enums.JobTitleType;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.DepartmentService;
import com.learning.emsmybatisliquibase.service.ProfileService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileWorker {

    private final ProfileService profileService;

    private final DepartmentService departmentService;

    private final ObjectMapper objectMapper;

    @JobWorker(type = "create-profile")
    public void createProfile(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey("employeeUuid")) {
            throw new InvalidInputException("INVALID_INPUT", "Invalid input: employeeUuid");
        }
        if (!data.containsKey("employeeDto")) {
            throw new InvalidInputException("INVALID_INPUT", "invalid input: employeeDto");
        }

        UUID employeeUuid = objectMapper.convertValue(data.get("employeeUuid"), UUID.class);
        AddEmployeeDto employeeDto = objectMapper.convertValue(data.get("employeeDto"), AddEmployeeDto.class);

        Department department = null;
        if (employeeDto.getDepartmentName() != null) {
            department = departmentService.add(new AddDepartmentDto(employeeDto.getDepartmentName().trim()));
        }
        try {
            profileService.insert(Profile.builder().
                    employeeUuid(employeeUuid)
                    .profileStatus(profileStatus(employeeDto))
                    .jobTitle(JobTitleType.valueOf(employeeDto.getJobTitle()))
                    .departmentUuid(department == null ? null : department.getUuid())
                    .updatedTime(LocalDateTime.now())
                    .build());
            log.info("Profile inserted for employee: {}", employeeUuid);
        } catch (Exception e) {
            log.error("Profile job worker failed with exception: {}", e.getMessage());
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("compensate-keycloak-employee-error")
                    .errorMessage("Invalid job title: " + employeeDto.getJobTitle())
                    .send();
        }
    }

    private ProfileStatus profileStatus(AddEmployeeDto employeeDto) {
        ProfileStatus profileStatus;
        boolean value = validatePasswords(employeeDto.getPassword(), employeeDto.getConfirmPassword());
        if (employeeDto.getLeavingDate() != null && employeeDto.getLeavingDate().isAfter(LocalDate.now())) {
            profileStatus = ProfileStatus.INACTIVE;
        } else if ((employeeDto.getLeavingDate() == null ||
                employeeDto.getLeavingDate().isBefore(LocalDate.now())) && !value) {
            profileStatus = ProfileStatus.PENDING;
        } else if ((employeeDto.getLeavingDate() == null ||
                employeeDto.getLeavingDate().isBefore(LocalDate.now())) && value) {
            profileStatus = ProfileStatus.ACTIVE;
        } else {
            profileStatus = ProfileStatus.PENDING;
        }
        return profileStatus;
    }

    private boolean validatePasswords(String password, String confirmPassword) {
        return StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(confirmPassword);
    }
}