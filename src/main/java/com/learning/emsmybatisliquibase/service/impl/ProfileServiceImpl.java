package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.Kafka.EmployeeDetailsEtmsProducer;
import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.ProfileDao;
import com.learning.emsmybatisliquibase.dto.EmployeeEtmsDetails;
import com.learning.emsmybatisliquibase.entity.Profile;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.ProfileErrorCodes.*;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileDao profileDao;

    private final EmployeeDetailsEtmsProducer employeeDetailsEtmsProducer;

    private final EmployeeDao employeeDao;

    public Profile getByEmployeeUuid(UUID employeeUuid) {
        var profile = profileDao.get(employeeUuid);
        if (profile == null) {
            throw new NotFoundException(PROFILE_NOT_FOUND.code(),
                    "Profile not found for colleague id: " + employeeUuid);
        }
        return profile;
    }


    @Override
    public Profile insert(Profile profile) {
        try {
            if (0 == profileDao.insert(profile)) {
                throw new NotFoundException(PROFILE_NOT_CREATED.code(),
                        "Failed in saving profile");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(PROFILE_NOT_CREATED.code(),
                    exception.getCause().getMessage());
        }
        sendEventToEtms(profile.getEmployeeUuid(), profile.getProfileStatus());
        return profile;
    }

    @Override
    public Profile update(Profile profileDto) {
        getByEmployeeUuid(profileDto.getEmployeeUuid());
        try {
            if (0 == profileDao.update(profileDto)) {
                throw new IntegrityException(PROFILE_NOT_UPDATED.code(),
                        "Profile not updated");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(PROFILE_NOT_UPDATED.code(),
                    exception.getCause().getMessage());
        }
        sendEventToEtms(profileDto.getEmployeeUuid(), profileDto.getProfileStatus());
        return profileDto;
    }

    private void sendEventToEtms(UUID employeeUuid, ProfileStatus profileStatus) {
        Runnable eventTask = () -> {
            if (profileStatus.equals(ProfileStatus.ACTIVE)) {
                setNewEmployeeDetailsProducer(employeeUuid, profileStatus);
            } else if (profileStatus.equals(ProfileStatus.INACTIVE)) {
                setUpdateEmployeeDetailsProducer(employeeUuid, profileStatus);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventTask.run();
                }
            });
        } else {
            eventTask.run();
        }
    }

    private void setNewEmployeeDetailsProducer(UUID employeeUuid, ProfileStatus profileStatus) {
        EmployeeEtmsDetails employee = employeeDao.getEtmsById(employeeUuid);
        if (employee == null) {
            return;
        }
        employee.setProfileStatus(profileStatus.toString());
        employee.setCreatedTime(Instant.now());
        employee.setUpdatedTime(Instant.now());
        employeeDetailsEtmsProducer.sendNewEmployee(employee);
    }

    private void setUpdateEmployeeDetailsProducer(UUID employeeUuid, ProfileStatus profileStatus) {
        EmployeeEtmsDetails employee = employeeDao.getEtmsById(employeeUuid);
        if (employee == null) {
            return;
        }
        employee.setProfileStatus(profileStatus.toString());
        employee.setUpdatedTime(Instant.now());
        employeeDetailsEtmsProducer.sendUpdateEmployee(employee);
    }

}
