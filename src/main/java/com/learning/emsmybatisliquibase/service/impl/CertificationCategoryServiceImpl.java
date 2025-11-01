package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.CertificationCategoryDao;
import com.learning.emsmybatisliquibase.dto.CertificationCategoryDto;
import com.learning.emsmybatisliquibase.entity.CertificationCategory;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.CertificationCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.config.CacheConfig.GET_ALL_CERTIFICATION_CATEGORY_CACHE;
import static com.learning.emsmybatisliquibase.config.CacheConfig.GET_CERTIFICATION_CATEGORY_BY_ID_CACHE;
import static com.learning.emsmybatisliquibase.exception.errorcodes.CertificationCategoryErrorCodes.*;

@Service
@RequiredArgsConstructor
public class CertificationCategoryServiceImpl implements CertificationCategoryService {

    private final CertificationCategoryDao certificationCategoryDao;

    @Override
    @Cacheable(value = GET_CERTIFICATION_CATEGORY_BY_ID_CACHE, key = "#id")
    public CertificationCategory getById(UUID id) {
        var certificationCategory = certificationCategoryDao.getById(id);

        if (certificationCategory == null) {
            throw new NotFoundException(CERTIFICATION_CATEGORY_NOT_FOUND.code(),
                    "Certification category not found with id: " + id);
        }

        return certificationCategory;
    }

    @Override
    @Cacheable(value = GET_ALL_CERTIFICATION_CATEGORY_CACHE)
    public List<CertificationCategory> getAll() {
        return certificationCategoryDao.getAll();
    }

    @Override
    @CachePut(value = GET_CERTIFICATION_CATEGORY_BY_ID_CACHE, key = "#result.uuid")
    @CacheEvict(value = GET_ALL_CERTIFICATION_CATEGORY_CACHE, allEntries = true)
    public CertificationCategory add(CertificationCategoryDto certificationCategoryDto) {
        var certificationCategoryByName = certificationCategoryDao.
                getByName(certificationCategoryDto.getName().trim());

        if (certificationCategoryByName != null) {
            throw new FoundException("CERTIFICATION_CATEGORY_ALREADY_EXISTS",
                    "Certification category already exists with name: " + certificationCategoryDto.getName());
        }
        var certificationCategory = CertificationCategory.builder()
                .uuid(UUID.randomUUID())
                .name(certificationCategoryDto.getName().trim())
                .build();

        try {
            if (0 == certificationCategoryDao.insert(certificationCategory)) {
                throw new IntegrityException(CERTIFICATION_CATEGORY_NOT_CREATED.code(),
                        "Certification category not created");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(CERTIFICATION_CATEGORY_NOT_CREATED.code(),
                    exception.getCause().getMessage());
        }

        return certificationCategory;
    }

    @Override
    @CachePut(value = GET_CERTIFICATION_CATEGORY_BY_ID_CACHE, key = "#id")
    @CacheEvict(value = GET_ALL_CERTIFICATION_CATEGORY_CACHE, allEntries = true)
    public CertificationCategory update(UUID id, CertificationCategory certificationCategoryDto) {
        if (!id.equals(certificationCategoryDto.getUuid())) {
            throw new InvalidInputException(CERTIFICATION_CATEGORY_INVALID_INPUT.code(), "Invalid input");
        }
        var certificationCategory = getById(id);
        certificationCategory.setName(certificationCategoryDto.getName().trim());

        try {
            if (0 == certificationCategoryDao.update(certificationCategory)) {
                throw new IntegrityException(CERTIFICATION_CATEGORY_NOT_UPDATED.code(),
                        "Certification category not updated with id: " + id);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(CERTIFICATION_CATEGORY_NOT_UPDATED.code(),
                    exception.getCause().getMessage());
        }

        return certificationCategory;
    }

    @Override
    @CacheEvict(value = {GET_CERTIFICATION_CATEGORY_BY_ID_CACHE, GET_ALL_CERTIFICATION_CATEGORY_CACHE}, key = "#id", allEntries = true)
    public void delete(UUID id) {
        getById(id);
        try {
            if (0 == certificationCategoryDao.delete(id)) {
                throw new IntegrityException(CERTIFICATION_CATEGORY_NOT_DELETED.code(),
                        "Certification Category not deleted with id: " + id);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(CERTIFICATION_CATEGORY_NOT_DELETED.code(),
                    exception.getCause().getMessage());
        }
    }

}
