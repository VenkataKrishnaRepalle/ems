package com.learning.emsmybatisliquibase.exception.errorcodes;

import com.learning.emsmybatisliquibase.exception.api.ErrorCodeAware;

public enum CertificationCategoryErrorCodes implements ErrorCodeAware {
    CERTIFICATION_CATEGORY_NOT_CREATED,
    CERTIFICATION_CATEGORY_NOT_FOUND,
    CERTIFICATION_CATEGORY_NOT_UPDATED,
    CERTIFICATION_CATEGORY_INVALID_INPUT,
    CERTIFICATION_CATEGORY_NOT_DELETED;

    @Override
    public String code() {
        return name();
    }
}
