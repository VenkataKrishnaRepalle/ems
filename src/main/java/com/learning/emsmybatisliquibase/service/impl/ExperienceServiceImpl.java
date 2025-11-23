package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.ExperienceDao;
import com.learning.emsmybatisliquibase.entity.Experience;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.ExperienceErrorCodes.*;

@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final ExperienceDao experienceDao;

    @Override
    public Experience getById(UUID experienceId) {
        var experience = experienceDao.getById(experienceId);
        if (experience == null) {
            throw new NotFoundException(EXPERIENCE_NOT_FOUND.code(), "experience not found with id: " + experienceId);
        }
        return experience;
    }

    @Override
    public List<Experience> getAllByEmployeeUuid(UUID employeeId) {
        return experienceDao.getByEmployeeUuid(employeeId);
    }

    @Override
    public List<Experience> add(UUID employeeUuid, List<Experience> experiences) {
        if (experiences == null) {
            throw new InvalidInputException(EXPERIENCE_INPUT_NULL.code(), "experiences input is null");
        }

        isInputOverlapping(experiences);
        isExistingExperienceOverlappingInputExperience(employeeUuid, experiences);

        experiences.forEach(experience -> {
            experience.setUuid(UUID.randomUUID());
            if (Boolean.TRUE.equals(experience.getIsCurrentJob())) {
                experience.setEndDate(null);
            } else if (experience.getEndDate() != null) {
                experience.setIsCurrentJob(Boolean.FALSE);
            }

            try {
                if (0 == experienceDao.insert(experience)) {
                    throw new IntegrityException(EXPERIENCE_NOT_CREATED.code(), "insertion failed");
                }
            } catch (DataIntegrityViolationException exception) {
                throw new IntegrityException(EXPERIENCE_NOT_CREATED.code(), exception.getCause().getMessage());
            }
        });

        return experiences;
    }

    @Override
    public List<Experience> update(UUID employeeUuid, List<Experience> experiences) {
        if (experiences == null) {
            throw new InvalidInputException(EXPERIENCE_INPUT_NULL.code(), "experiences input is null");
        }

        experiences.forEach(experience -> getById(experience.getUuid()));

        isInputOverlapping(experiences);

        experiences.forEach(experience -> {
            if (Boolean.TRUE.equals(experience.getIsCurrentJob())) {
                experience.setEndDate(null);
            } else if (experience.getEndDate() != null) {
                experience.setIsCurrentJob(Boolean.FALSE);
            }
            try {
                if (0 == experienceDao.update(experience)) {
                    throw new IntegrityException(EXPERIENCE_NOT_UPDATED.code(),
                            "experience not updated with id: " + experience.getUuid());
                }
            } catch (DataIntegrityViolationException exception) {
                throw new IntegrityException(EXPERIENCE_NOT_UPDATED.code(),
                        exception.getCause().getMessage());
            }
        });

        return experiences;
    }

    @Override
    public void deleteById(UUID experienceId) {
        getById(experienceId);
        try {
            if (0 == experienceDao.delete(experienceId)) {
                throw new IntegrityException(EXPERIENCE_NOT_DELETED.code(),
                        "experience not deleted with id: " + experienceId);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EXPERIENCE_NOT_DELETED.code(),
                    exception.getCause().getMessage());
        }
        var isExperienceDeleted = experienceDao.getById(experienceId);
        if (isExperienceDeleted != null) {
            throw new FoundException(EXPERIENCE_NOT_DELETED.code(),
                    "experience not deleted with id: " + experienceId);
        }
    }

    private void isInputOverlapping(List<Experience> experiences) {
        for (int i = 0; i < experiences.size() - 1; i++) {
            for (int j = i + 1; j < experiences.size(); j++) {
                if (isOverlapping(experiences.get(i), experiences.get(j))) {
                    throw new InvalidInputException(EXPERIENCE_INPUT_OVERLAPPING.code(),
                            "experience date is overlapping, Please check and update");
                }
            }
        }
    }

    private void isExistingExperienceOverlappingInputExperience(UUID employeeUuid, List<Experience> experiences) {
        var employeeExperiences = experienceDao.getByEmployeeUuid(employeeUuid);
        if (employeeExperiences != null) {
            for (Experience experience : experiences) {
                boolean isOverlapping = employeeExperiences.stream().anyMatch(existingExperience ->
                        isOverlapping(experience, existingExperience)
                );
                if (isOverlapping) {
                    throw new InvalidInputException(EXPERIENCE_OVERLAPPING.code(),
                            "input experience dates are overlapping, please check and Update");
                }
            }
        }
    }

    private boolean isOverlapping(Experience newExperience, Experience existingExperience) {
        LocalDateTime newStart = convertToLocalDateTime(newExperience.getStartDate());
        LocalDateTime newEnd;
        if (Boolean.TRUE.equals(newExperience.getIsCurrentJob())) {
            newEnd = LocalDateTime.now();
        } else {
            newEnd = convertToLocalDateTime(newExperience.getEndDate());
        }
        LocalDateTime existingStart = convertToLocalDateTime(existingExperience.getStartDate());
        LocalDateTime existingEnd;

        if (Boolean.TRUE.equals(existingExperience.getIsCurrentJob())) {
            existingEnd = LocalDateTime.now();
        } else {
            existingEnd = convertToLocalDateTime(existingExperience.getEndDate());
        }

        return (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart));
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
