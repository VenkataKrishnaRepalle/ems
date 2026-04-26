package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.PasswordDao;
import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.dto.pagination.KeycloakCredentialsDto;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.Password;
import com.learning.emsmybatisliquibase.entity.enums.OtpAuthType;
import com.learning.emsmybatisliquibase.entity.enums.PasswordStatus;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.KeycloakService;
import com.learning.emsmybatisliquibase.service.OtpService;
import com.learning.emsmybatisliquibase.service.PasswordService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.*;

@Service
@AllArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final PasswordDao passwordDao;

    private final EmployeeDao employeeDao;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final OtpService otpService;

    private final KeycloakService keycloakService;

    public Password getById(UUID uuid) {
        var password = passwordDao.getById(uuid);
        if (password == null) {
            throw new IntegrityException("PASSWORD_NOT_FOUND", "Password not found with id: " + uuid);
        }
        return password;
    }

    @Override
    public void create(UUID employeeUuid, PasswordDto passwordDto) {
        validatePassword(passwordDto.getPassword(), passwordDto.getConfirmPassword());

        insert(employeeUuid, passwordDto.getPassword());
    }

    private void insert(UUID employeeUuid, String passwordInput) {
        var password = Password.builder()
                .uuid(UUID.randomUUID())
                .employeeUuid(employeeUuid)
                .password(passwordEncoder.encode(passwordInput))
                .status(PasswordStatus.ACTIVE)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        try {
            if (0 == passwordDao.insert(password)) {
                throw new IntegrityException("PASSWORD_NOT_INSERTED", "Password failed to create");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PASSWORD_NOT_INSERTED", "Password failed to create");
        }
    }

    private void validatePassword(String password, String confirmPassword) {
        if (StringUtils.isNotEmpty(password) && password.equals(confirmPassword)) {
            if (password.length() < 8) {
                throw new InvalidInputException(EMPLOYEE_PASSWORD_LENGTH_TOO_SHORT.code(),
                        "Password length should be at least 8 characters");
            }
        } else {
            throw new InvalidInputException(EMPLOYEE_PASSWORD_MISMATCH.code(),
                    "Password and Confirm Password should be the same");
        }
    }

    @Override
    public Password update(Password password) {
        getById(password.getUuid());
        var employee = employeeDao.get(password.getEmployeeUuid());
        try {
            if (0 == passwordDao.update(password)) {
                throw new IntegrityException("PASSWORD_UPDATE_FAILED", "Password not updated");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("PASSWORD_UPDATE_FAILED", "Password not updated");
        }

        if(password.getNoOfIncorrectEntries() >= 3) {
            updateKeycloak(employee, null, false);
        }
        updateKeycloak(employee, password.getPassword(), true);


        return password;
    }

    private void updateKeycloak(Employee employee, String password, boolean enabled) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(employee.getKeycloakUserUuid().toString());
        userRepresentation.setUsername(employee.getUsername());
        userRepresentation.setEmail(employee.getEmail());
        userRepresentation.setFirstName(employee.getFirstName());
        userRepresentation.setLastName(employee.getLastName());
        userRepresentation.setEmail(employee.getEmail());
        userRepresentation.setEnabled(enabled);

        if(password!=null) {
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(password);
            credentialRepresentation.setTemporary(false);

            userRepresentation.setCredentials(List.of(credentialRepresentation));

        }

        Thread t = new Thread(() -> {
            keycloakService.update(userRepresentation);
        });
        t.start();
    }

    @Transactional
    @Override
    public SuccessResponseDto forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        var employee = getByEmail(forgotPasswordDto.getEmail());

        otpService.verifyOtp(employee.getUuid(), forgotPasswordDto.getOtp().trim(), OtpAuthType.FORGOT_PASSWORD);

        var passwords = passwordDao.getByEmployeeUuidAndStatus(employee.getUuid(), PasswordStatus.ACTIVE);
        passwords.forEach(password -> {
            password.setStatus(PasswordStatus.EXPIRED);
            update(password);
        });
        create(employee.getUuid(), PasswordDto.builder()
                .password(forgotPasswordDto.getPassword())
                .confirmPassword(forgotPasswordDto.getConfirmPassword())
                .build());

        updateKeycloak(employee, forgotPasswordDto.getPassword(), false);

        return SuccessResponseDto.builder()
                .success(Boolean.TRUE)
                .data(employee.getUuid().toString())
                .build();
    }

    @Transactional
    @Override
    public SuccessResponseDto resetPassword(ResetPasswordDto resetPasswordDto) {
        var employee = getByEmail(resetPasswordDto.getEmail().trim());
        var passwords = passwordDao.getByEmployeeUuidAndStatus(employee.getUuid(), PasswordStatus.ACTIVE);
        if (!passwordEncoder.matches(resetPasswordDto.getOldPassword(), passwords.getFirst().getPassword())) {
            throw new InvalidInputException(PASSWORD_NOT_MATCHED.code(), "Entered Password in Incorrect");
        }
        passwords.forEach(password -> {
            password.setStatus(PasswordStatus.EXPIRED);
            update(password);
        });
        create(employee.getUuid(), PasswordDto.builder()
                .password(resetPasswordDto.getNewPassword())
                .confirmPassword(resetPasswordDto.getConfirmNewPassword())
                .build());

        updateKeycloak(employee, resetPasswordDto.getNewPassword(), false);

        return SuccessResponseDto.builder()
                .success(Boolean.TRUE)
                .data(employee.getUuid().toString())
                .build();
    }

    private Employee getByEmail(String email) {
        var employee = employeeDao.getByEmail(email);
        if (employee == null) {
            throw new IntegrityException("EMPLOYEE_NOT_FOUND", "Employee not found with email: " + email);
        }
        return employee;
    }

}
