package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.CertificationDao;
import com.learning.emsmybatisliquibase.dto.CertificationRequestDto;
import com.learning.emsmybatisliquibase.dto.CertificationCategoryDto;
import com.learning.emsmybatisliquibase.dto.CertificationResponseDto;
import com.learning.emsmybatisliquibase.entity.Certification;
import com.learning.emsmybatisliquibase.entity.CertificationCategory;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.mapper.CertificationMapper;
import com.learning.emsmybatisliquibase.service.CertificationCategoryService;
import com.learning.emsmybatisliquibase.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements CertificationService {

    private final CertificationDao certificationDao;

    private final CertificationCategoryService certificationCategoryService;

    private final CertificationMapper certificationMapper;

    @Override
    public List<CertificationResponseDto> getAllByEmployeeUuid(UUID employeeUuid) {
        var certifications = certificationDao.getByEmployeeUuid(employeeUuid);
        List<CertificationResponseDto> response = new ArrayList<>();
        if (certifications == null) {
            return List.of();
        }

        for (var certification : certifications) {
            var certificationCategory = certificationCategoryService
                    .getById(certification.getCertificationCategoryUuid());
            var certificationResponse = certificationMapper
                    .certificationToCertificationResponseDto(certification);
            certificationResponse.setCertificationCategory(certificationCategory);
            response.add(certificationResponse);
        }
        return response;
    }

    @Override
    public CertificationResponseDto add(CertificationRequestDto addCertificationDto) {
        isCertificationExistsWithName(addCertificationDto.getName().strip(),
                addCertificationDto.getEmployeeUuid());
        var certificationCategory = findOrCreateCertificationCategory(addCertificationDto);

        var certification = certificationMapper.addCertificationDtoToCertification(addCertificationDto);
        certification.setUuid(UUID.randomUUID());
        certification.setName(certification.getName().strip());
        certification.setCertificationCategoryUuid(certificationCategory.getUuid());
        certification.setCreationTime(LocalDateTime.now());
        certification.setUpdatedTime(LocalDateTime.now());

        insert(certification);

        return certificationToCertificationResponse(certification, certificationCategory);
    }

    @Override
    public CertificationResponseDto getById(UUID certificationUuid) {
        var certification = get(certificationUuid);
        return certificationToCertificationResponse(certification,
                certificationCategoryService.getById(certification.getCertificationCategoryUuid()));
    }

    @Override
    public CertificationResponseDto update(UUID certificationUuid,
                                           CertificationRequestDto updateCertificationDto) {
        isCertificationExistsWithName(updateCertificationDto.getName().strip(),
                updateCertificationDto.getEmployeeUuid());
        var certificationCategory = findOrCreateCertificationCategory(updateCertificationDto);
        var certification = get(certificationUuid);
        certification.setCertificationCategoryUuid(certificationCategory.getUuid());
        certification.setEmployeeUuid(updateCertificationDto.getEmployeeUuid());
        certification.setName(updateCertificationDto.getName().strip());
        certification.setSkillSets(updateCertificationDto.getSkillSets());
        certification.setCertifiedDate(updateCertificationDto.getCertifiedDate());
        certification.setExpiryDate(updateCertificationDto.getExpiryDate());
        certification.setLevel(updateCertificationDto.getLevel());
        certification.setUpdatedTime(LocalDateTime.now());

        try {
            if (0 == certificationDao.update(certification)) {
                throw new IntegrityException("CERTIFICATION_UPDATE_FAILED",
                        "Failed to update certification with id: " + certificationUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("CERTIFICATION_UPDATE_FAILED",
                    "Failed to update certification with id: " + certificationUuid);
        }

        return certificationToCertificationResponse(certification, certificationCategory);
    }

    @Override
    public void delete(UUID certificationUuid) {
        get(certificationUuid);

        try {
            if (0 == certificationDao.delete(certificationUuid)) {
                throw new IntegrityException("CERTIFICATION_DELETE_FAILED",
                        "Failed to delete certification with id: " + certificationUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("CERTIFICATION_DELETE_FAILED",
                    "Failed to delete certification with id: " + certificationUuid);
        }
    }

    @Override
    public Map<UUID, String> getByCertificationCategory(UUID certificationCategoryUuid) {
        var certifications = certificationDao.getByCertificationCategoryUuid(certificationCategoryUuid);
        Map<UUID, String> certificationMap = new HashMap<>();
        for (var certification : certifications) {
            certificationMap.put(certification.getUuid(), certification.getName());
        }
        return certificationMap;
    }

    private void isCertificationExistsWithName(String name, UUID employeeUuid) {
        var isCertificationExists = certificationDao.getByNameAndEmployeeUuid(name, employeeUuid);
        if (isCertificationExists != null) {
            throw new IntegrityException("CERTIFICATION_ALREADY_EXISTS",
                    "Certification with the same name already exists for the given employee: " + name);
        }
    }

    private Certification get(UUID certificationUuid) {
        var certification = certificationDao.getById(certificationUuid);
        if (certification == null) {
            throw new InvalidInputException("CERTIFICATION_NOT_FOUND",
                    "Certification not found with id: " + certificationUuid);
        }
        return certification;
    }

    private CertificationCategory findOrCreateCertificationCategory(CertificationRequestDto addCertificationDto) {
        if (addCertificationDto.getCertificationCategoryUuid() != null) {
            CertificationCategory certificationCategory = certificationCategoryService
                    .getById(addCertificationDto.getCertificationCategoryUuid());
            if (certificationCategory != null) {
                return certificationCategory;
            } else if (addCertificationDto.getCertificationCategoryName() == null) {
                throw new InvalidInputException("CERTIFICATION_CATEGORY_NOT_FOUND",
                        "Certification category not found with id: " + addCertificationDto.getCertificationCategoryUuid());
            }
        }
        if (addCertificationDto.getCertificationCategoryName() != null) {
            return certificationCategoryService.add(
                    CertificationCategoryDto
                            .builder()
                            .name(addCertificationDto.getCertificationCategoryName())
                            .build()
            );
        }
        throw new InvalidInputException("CERTIFICATION_CATEGORY_NAME_NOT_FOUND",
                "Certification category name is required when UUID is not found.");
    }

    private void insert(Certification certification) {
        try {
            if (0 == certificationDao.insert(certification)) {
                throw new IntegrityException("CERTIFICATION_NOT_SAVED", "Certification not saved");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("CERTIFICATION_NOT_SAVED", exception.getCause().getMessage());
        }
    }

    private CertificationResponseDto certificationToCertificationResponse(Certification certification,
                                                                          CertificationCategory certificationCategory) {
        var response = certificationMapper.certificationToCertificationResponseDto(certification);
        response.setCertificationCategory(certificationCategory);
        return response;
    }

}