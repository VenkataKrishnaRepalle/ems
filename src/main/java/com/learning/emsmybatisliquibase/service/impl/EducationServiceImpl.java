package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EducationDao;
import com.learning.emsmybatisliquibase.entity.Education;
import com.learning.emsmybatisliquibase.entity.enums.EducationDegree;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.EducationService;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EducationErrorCodes.*;

@Service
@AllArgsConstructor
public class EducationServiceImpl implements EducationService {

    private final EducationDao educationDao;

    private final EmployeeService employeeService;

    @Override
    public Education save(Education educationDto) {
        validateEducation(educationDto);
        employeeService.getById(educationDto.getEmployeeUuid());

        var educations = educationDao.getAllByEmployeeUuid(educationDto.getEmployeeUuid());
        var degree = educationDto.getDegree();

        if (educations.isEmpty()) {
            if (!degree.equals(EducationDegree.SSC_10TH)) {
                throw new NotFoundException(EDUCATION_DETAILS_NOT_FOUND.code(),
                        "Education Details of " + EducationDegree.SSC_10TH + " not found");
            }
        } else {
            var isDegreePresent = educations.stream()
                    .filter(education -> education.getDegree().equals(educationDto.getDegree()))
                    .findFirst();
            if (isDegreePresent.isPresent()) {
                throw new FoundException(EDUCATION_DEGREE_ALREADY_EXISTS.code(),
                        "Degree already exists: " + degree);
            }
        }
        educationDto.setUuid(UUID.randomUUID());
        educationDto.setCreatedTime(LocalDateTime.now());
        educationDto.setUpdatedTime(LocalDateTime.now());

        try {
            if (0 == educationDao.insert(educationDto)) {
                throw new IntegrityException(EDUCATION_NOT_CREATED.code(),
                        "Education Details not created");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EDUCATION_NOT_CREATED.code(),
                    exception.getCause().getMessage());
        }

        return educationDto;
    }

    private void validateEducation(Education educationDto) {
        if (educationDto.getStartDate().isEqual(educationDto.getEndDate())) {
            throw new InvalidInputException(INVALID_EDUCATION_START_END_DATE.code(),
                    "Provided input start and end time are same");
        } else if (educationDto.getEndDate().isBefore(educationDto.getStartDate())) {
            throw new InvalidInputException(INVALID_EDUCATION_START_END_DATE.code(),
                    "Provided input start and end time is not valid");
        }
    }

    @Override
    public Education update(Education educationDto, UUID id) {
        getById(educationDto.getUuid());
        validateEducation(educationDto);
        educationDto.setUpdatedTime(LocalDateTime.now());

        try {
            if (0 == educationDao.update(educationDto)) {
                throw new IntegrityException(EDUCATION_NOT_UPDATED.code(),
                        "Education Details not updated");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EDUCATION_NOT_UPDATED.code(),
                    exception.getCause().getMessage());
        }

        return educationDto;
    }

    @Override
    public Education getById(UUID id) {
        var education = educationDao.get(id);
        if (education == null) {
            throw new NotFoundException(EDUCATION_DETAILS_NOT_FOUND.code(),
                    "Education details not found with id: " + id);
        }
        return education;
    }

    @Override
    public List<Education> getAll(UUID employeeId) {
        List<Education> educations = educationDao.getAllByEmployeeUuid(employeeId);
        if (educations == null) {
            throw new NotFoundException(EDUCATION_DETAILS_NOT_FOUND.code(),
                    "Education details not found");
        }
        return educations.stream()
                .sorted(Comparator.comparing(Education::getStartDate))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        getById(id);

        try {
            if (0 == educationDao.delete(id)) {
                throw new IntegrityException(EDUCATION_NOT_DELETED.code(),
                        "Education Details not deleted");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EDUCATION_NOT_DELETED.code(),
                    exception.getCause().getMessage());
        }

    }
}
