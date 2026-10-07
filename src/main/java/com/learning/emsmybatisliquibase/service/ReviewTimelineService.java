package com.learning.emsmybatisliquibase.service;


import com.learning.emsmybatisliquibase.dto.FullEmployeePeriodDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.dto.TimelineAndReviewResponseDto;
import com.learning.emsmybatisliquibase.entity.ReviewTimeline;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;

import java.util.List;
import java.util.UUID;

public interface ReviewTimelineService {

    ReviewTimeline getById(UUID uuid);

    FullEmployeePeriodDto getActiveTimelineDetails(UUID employeeId);

    void updateTimelineStatus(List<UUID> employeeUuids, ReviewType reviewType, ReviewTimelineStatus reviewTimelineStatus);

    SuccessResponseDto startTimelinesForQuarter(ReviewType completedReviewType, ReviewType startedReviewType);

    ReviewTimeline getByEmployeePeriodIdAndReviewType(UUID employeePeriodUuid, ReviewType reviewType);

    TimelineAndReviewResponseDto getByEmployeePeriodAndReviewType(UUID employeePeriodUuid, ReviewType reviewType);

    int update(UUID periodUuid, ReviewType reviewType, ReviewTimelineStatus reviewTimelineStatus);
}
