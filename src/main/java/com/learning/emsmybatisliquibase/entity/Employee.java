package com.learning.emsmybatisliquibase.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Employee implements Serializable {

    private UUID uuid;

    private String firstName;

    private String lastName;

    private Gender gender;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String username;

    private String email;

    private UUID managerUuid;

    private Boolean isManager;

    private LocalDate joiningDate;

    private LocalDate leavingDate;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime createdTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updatedTime;

    private RequestQuery difference(Employee employee) {
        Map<String, String> oldValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();
        StringBuilder changes = new StringBuilder();

        if (!firstName.equalsIgnoreCase(employee.getFirstName())) {
            oldValues.put("firstName", firstName);
            newValues.put("firstName", employee.getFirstName());
            changes.append("First name, ");
        }
        if (!lastName.equalsIgnoreCase(employee.getLastName())) {
            oldValues.put("lastName", lastName);
            newValues.put("lastName", employee.getLastName());
            changes.append("Last name, ");
        }
        if (!gender.equals(employee.getGender())) {
            oldValues.put("gender", gender.name());
            newValues.put("gender", employee.getGender().name());
            changes.append("Gender, ");
        }
        if (!dateOfBirth.isEqual(employee.getDateOfBirth())) {
            oldValues.put("dateOfBirth", dateOfBirth.toString());
            newValues.put("dateOfBirth", employee.getDateOfBirth().toString());
            changes.append("Date of birth, ");
        }
        if (!phoneNumber.equals(employee.getPhoneNumber())) {
            oldValues.put("phoneNumber", phoneNumber);
            newValues.put("phoneNumber", employee.getPhoneNumber());
            changes.append("Phone number, ");
        }
        if (!username.equals(employee.getUsername())) {
            oldValues.put("username", username);
            newValues.put("username", employee.getUsername());
            changes.append("Username, ");
        }
        if (!email.equals(employee.getEmail())) {
            oldValues.put("email", email);
            newValues.put("email", employee.getEmail());
            changes.append("Email, ");
        }
        if ( managerUuid != null && employee.getManagerUuid() != null && !managerUuid.equals(employee.getManagerUuid())) {
            oldValues.put("managerUuid", managerUuid.toString());
            newValues.put("managerUuid", employee.getManagerUuid().toString());
            changes.append("Manager, ");
        }
        if (!isManager.equals(employee.getIsManager())) {
            oldValues.put("isManager", isManager.toString());
            newValues.put("isManager", employee.getIsManager().toString());
            changes.append("Manager toggle, ");
        }
        if (!joiningDate.isEqual(employee.getJoiningDate())) {
            oldValues.put("joiningDate", joiningDate.toString());
            newValues.put("joiningDate", employee.getJoiningDate().toString());
            changes.append("Joining date, ");
        }
        if (!leavingDate.isEqual(employee.getLeavingDate())) {
            oldValues.put("leavingDate", leavingDate.toString());
            newValues.put("leavingDate", employee.getLeavingDate().toString());
            changes.append("Leaving date, ");
        }
        RequestQuery result = new RequestQuery();
        result.setProperty("oldValues", oldValues);
        result.setProperty("newValues", newValues);
        result.setProperty("changes", changes);
        return result;
    }
}