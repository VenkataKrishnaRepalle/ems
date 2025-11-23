package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.SkillsDao;
import com.learning.emsmybatisliquibase.dto.SkillsDto;
import com.learning.emsmybatisliquibase.entity.Skills;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.SkillsMapper;
import com.learning.emsmybatisliquibase.service.SkillsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.SkillsErrorCodes.INVALID_RATINGS_INPUT;
import static com.learning.emsmybatisliquibase.exception.errorcodes.SkillsErrorCodes.SKILLS_ALREADY_EXISTS;
import static com.learning.emsmybatisliquibase.exception.errorcodes.SkillsErrorCodes.SKILLS_NOT_CREATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.SkillsErrorCodes.SKILLS_NOT_DELETED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.SkillsErrorCodes.SKILLS_NOT_EXISTS;
import static com.learning.emsmybatisliquibase.exception.errorcodes.SkillsErrorCodes.SKILLS_NOT_UPDATED;


@Service
@RequiredArgsConstructor
public class SkillsServiceImpl implements SkillsService {

    private final SkillsMapper skillsMapper;

    private final SkillsDao skillsDao;

    @Override
    public Skills add(SkillsDto skillsDto) {
        inputValidation(skillsDto);
        isSkillExists(skillsDto.getName().trim(), skillsDto.getEmployeeUuid());

        var skills = skillsMapper.skillsDtoToSkills(skillsDto);
        skills.setName(skills.getName().trim());
        skills.setUuid(UUID.randomUUID());
        skills.setCreatedTime(LocalDateTime.now());
        skills.setUpdatedTime(LocalDateTime.now());

        try {
            if (0 == skillsDao.insert(skills)) {
                throw new IntegrityException(SKILLS_NOT_CREATED.code(), "skill not created");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(SKILLS_NOT_CREATED.code(), exception.getCause().getMessage());
        }

        return skills;
    }


    @Override
    public Skills update(UUID skillsUuid, SkillsDto skillsDto) {
        inputValidation(skillsDto);
        var skills = getById(skillsUuid);
        skills.setName(skillsDto.getName());
        skills.setRating(skillsDto.getRating());
        skills.setUpdatedTime(LocalDateTime.now());
        try {
            if (0 == skillsDao.update(skills)) {
                throw new IntegrityException(SKILLS_NOT_UPDATED.code(),
                        "Skills not updated for skillsId: " + skillsUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(SKILLS_NOT_UPDATED.code(), exception.getCause().getMessage());
        }

        return skills;
    }

    @Override
    public Skills getById(UUID skillsUuid) {
        var skill = skillsDao.getById(skillsUuid);
        if (skill == null) {
            throw new NotFoundException(SKILLS_NOT_EXISTS.code(),
                    "Skills not found with id " + skillsUuid);
        }
        return skill;
    }

    @Override
    public List<Skills> getAll(UUID employeeUuid) {
        return skillsDao.getByEmployeeId(employeeUuid);
    }

    @Override
    public void deleteById(UUID skillsUuid, UUID employeeUuid) {
        getById(skillsUuid);
        try {
            if (0 == skillsDao.delete(skillsUuid)) {
                throw new IntegrityException(SKILLS_NOT_DELETED.code(),
                        "Skills not deleted for skillsId: " + skillsUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(SKILLS_NOT_DELETED.code(), exception.getCause().getMessage());
        }
    }


    private void inputValidation(SkillsDto skillsDto) {
        if (StringUtils.isBlank(skillsDto.getName())) {
            throw new InvalidInputException(INVALID_RATINGS_INPUT.code(),
                    "Name cannot be empty or null");
        }
        if (skillsDto.getRating() < 1 || skillsDto.getRating() > 10) {
            throw new InvalidInputException(INVALID_RATINGS_INPUT.code(),
                    "Ratings must between 1 and 10");
        }
    }

    private void isSkillExists(String name, UUID employeeUuid) {
        var skills = skillsDao.getByNameAndEmployeeId(name, employeeUuid);

        if (skills.isEmpty()) {
            return;
        }
        for (var skill : skills) {
            if (name.equalsIgnoreCase(skill.getName().toLowerCase())) {
                throw new InvalidInputException(SKILLS_ALREADY_EXISTS.code(),
                        "Skill already exists");
            }
        }
    }
}
