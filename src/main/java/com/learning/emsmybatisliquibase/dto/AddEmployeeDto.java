package com.learning.emsmybatisliquibase.dto;

import com.learning.emsmybatisliquibase.entity.enums.Gender;
import com.learning.emsmybatisliquibase.entity.enums.JobTitleType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddEmployeeDto {

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String email;

    private LocalDate joiningDate;

    private LocalDate leavingDate;

    private String departmentName;

    private String isManager;

    private UUID managerUuid;

    @Enumerated(EnumType.STRING)
    private String jobTitle;

    private String password;

    private String confirmPassword;

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (this.getFirstName() == null || this.getFirstName().trim().isEmpty()) {
            errors.add("First Name is Required");
        }
        if (this.getLastName() == null || this.getLastName().trim().isEmpty()) {
            errors.add("Last Name is Required");
        }
        if (this.getGender() == null) {
            errors.add("Gender is Required");
        }
        if (this.getDateOfBirth() == null) {
            errors.add("Date of Birth is Required");
        }
        if (this.getPhoneNumber() == null || this.getPhoneNumber().trim().isEmpty()) {
            errors.add("Phone Number is Required");
        } else if (this.getPhoneNumber().length() != 10) {
            errors.add("Phone Number length should be 10 characters");
        }

        if (this.getEmail() == null || this.getEmail().trim().isEmpty()) {
            errors.add("Email is Required");
        } else {
            boolean valid = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
                    .matcher(this.getEmail().trim())
                    .matches();

            if (!valid) {
                errors.add("Invalid Email: " + this.getEmail());
            }
        }
        if (this.getJoiningDate() == null) {
            errors.add("Joining Date is Required");
        }

        if (this.departmentName == null || this.getDepartmentName().trim().isEmpty()) {
            errors.add("Department Name is Required");
        }

        if (this.getJobTitle() == null || this.getJobTitle().trim().isEmpty()) {
            errors.add("Job Title is Required");
        } else {
            try {
                JobTitleType.valueOf(this.getJobTitle());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid Job Title: " + this.getJobTitle());
            }
        }
        return errors;
    }
}