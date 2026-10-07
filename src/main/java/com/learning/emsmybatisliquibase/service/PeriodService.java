package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.entity.Period;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;

import java.util.UUID;

public interface PeriodService {

    Period createPeriod(int year);

    SuccessResponseDto startScheduled(UUID periodId);

    Period getById(UUID periodId);

    SuccessResponseDto updateStatus(UUID periodId, PeriodStatus status);

    Period update(Period period);

    Period getCurrentActivePeriod();
}
