package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeePeriodDao;
import com.learning.emsmybatisliquibase.dao.ReviewDao;
import com.learning.emsmybatisliquibase.dao.ReviewTimelineDao;
import com.learning.emsmybatisliquibase.dto.FullEmployeePeriodDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.dto.TimelineAndReviewResponseDto;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import com.learning.emsmybatisliquibase.entity.ReviewTimeline;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.EmployeePeriodMapper;
import com.learning.emsmybatisliquibase.mapper.ReviewTimelineMapper;
import com.learning.emsmybatisliquibase.service.CommunicationService;
import com.learning.emsmybatisliquibase.service.ReviewTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeePeriodErrorCodes.EMPLOYEE_PERIOD_NOT_FOUND;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_NOT_FOUND;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_NOT_UPDATED;

@Service
@RequiredArgsConstructor
public class ReviewTimelineServiceImpl implements ReviewTimelineService {

    private final ReviewTimelineDao reviewTimelineDao;

    private final EmployeePeriodDao employeePeriodDao;

    private final EmployeePeriodMapper employeePeriodMapper;

    private final ReviewTimelineMapper reviewTimelineMapper;

    private final CommunicationService communicationService;

    private final ReviewDao reviewDao;

    public ReviewTimeline getById(UUID uuid) {
        var timeline = reviewTimelineDao.getById(uuid);
        if (timeline == null) {
            throw new NotFoundException(TIMELINE_NOT_FOUND.code(), "Timeline not found with id: " + uuid);
        }
        return timeline;
    }

    @Override
    public FullEmployeePeriodDto getActiveTimelineDetails(UUID employeeId) {
        var employeeCycle = employeePeriodDao.getActivePeriodByEmployeeId(employeeId);
        if (employeeCycle == null) {
            throw new NotFoundException(EMPLOYEE_PERIOD_NOT_FOUND.code(),
                    "Active Employee Cycle not found for employeeId: " + employeeId);
        }
        var fullTimeline = employeePeriodMapper
                .employeePeriodToFullEmployeePeriodDto(employeeCycle);
        fullTimeline.setReviewTimelines(reviewTimelineDao
                .getByEmployeePeriodId(employeeCycle.getUuid()));
        return fullTimeline;
    }

    @Override
    public void updateTimelineStatus(List<UUID> employeeUuids, ReviewType reviewType,
                                     ReviewTimelineStatus reviewTimelineStatus) {
        var timelines = reviewTimelineDao
                .getByEmployeeUuidsAndReviewType(employeeUuids, reviewType);

        timelines.forEach(timeline -> {
            timeline.setStatus(reviewTimelineStatus);
            update(timeline);
        });

        SuccessResponseDto.builder()
                .data(UUID.randomUUID().toString())
                .success(Boolean.TRUE)
                .build();
    }

    @Override
    public SuccessResponseDto startTimelinesForQuarter(ReviewType completedReviewType, ReviewType startedReviewType) {
        var completedReviewTimelines = reviewTimelineDao.findByStatusAndReviewType(PeriodStatus.STARTED,
                completedReviewType);
        completedReviewTimelines.forEach(timeline -> {
            timeline.setStatus(ReviewTimelineStatus.COMPLETED);
            timeline.setUpdatedTime(Instant.now());
            update(timeline);
        });

        List<ReviewTimeline> startedReviewTimelines = reviewTimelineDao.findByStatusAndReviewType(PeriodStatus.SCHEDULED,
                startedReviewType);

        startedReviewTimelines.forEach(timeline -> {
            timeline.setStatus(ReviewTimelineStatus.STARTED);
            timeline.setUpdatedTime(Instant.now());
            update(timeline);
        });

        communicationService.sendStartNotification(startedReviewType);

        return SuccessResponseDto.builder()
                .data(UUID.randomUUID().toString())
                .success(Boolean.TRUE)
                .build();
    }

    @Override
    public TimelineAndReviewResponseDto getByEmployeePeriodAndReviewType(UUID employeePeriodUuid, ReviewType reviewType) {
        var timeline = getByEmployeePeriodIdAndReviewType(employeePeriodUuid, reviewType);
        var review = reviewDao.getByTimelineId(timeline.getUuid());

        var response = reviewTimelineMapper.timelineToTimelineAndReviewResponseDto(timeline);
        response.setReview(review);
        return response;
    }

    @Override
    public ReviewTimeline getByEmployeePeriodIdAndReviewType(UUID employeePeriodId, ReviewType reviewType) {
        var reviewTimeline = reviewTimelineDao.getByEmployeePeriodIdAndReviewType(employeePeriodId, reviewType);
        if (reviewTimeline == null) {
            throw new NotFoundException(TIMELINE_NOT_FOUND.code(), "Timeline not found with id: " + employeePeriodId);
        }
        return reviewTimeline;
    }

    private void update(ReviewTimeline reviewTimeline) {
        try {
            if (0 == reviewTimelineDao.update(reviewTimeline)) {
                throw new IntegrityException(TIMELINE_NOT_UPDATED.code(),
                        "Timeline not updated for id: " + reviewTimeline.getUuid());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(TIMELINE_NOT_UPDATED.code(), exception.getCause().getMessage());
        }
    }
}
