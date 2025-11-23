package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.PeriodDao;
import com.learning.emsmybatisliquibase.dao.EmployeePeriodDao;
import com.learning.emsmybatisliquibase.dao.ReviewTimelineDao;
import com.learning.emsmybatisliquibase.dto.EmployeeCycleAndTimelineResponseDto;
import com.learning.emsmybatisliquibase.dto.FullEmployeePeriodDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.entity.*;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.mapper.EmployeePeriodMapper;
import com.learning.emsmybatisliquibase.service.EmployeePeriodService;
import com.learning.emsmybatisliquibase.service.ReviewTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.learning.emsmybatisliquibase.exception.errorcodes.PeriodErrorCodes.PERIOD_NOT_EXISTS;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeePeriodErrorCodes.EMPLOYEE_PERIOD_NOT_CREATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeePeriodErrorCodes.EMPLOYEE_PERIOD_NOT_UPDATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_NOT_UPDATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_NOT_ASSIGNED;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeePeriodServiceImpl implements EmployeePeriodService {

    private final EmployeePeriodDao employeePeriodDao;

    private final PeriodDao periodDao;

    private final ReviewTimelineDao reviewTimelineDao;

    private final ReviewTimelineService reviewTimelineService;

    private final EmployeePeriodMapper employeePeriodMapper;

    private Period getCurrentActivePeriod() {
        var period = periodDao.getByStatus(PeriodStatus.STARTED);

        if (period == null) {
            throw new NotFoundException(PERIOD_NOT_EXISTS.code(),
                    "No Active Period for Current Period");
        }
        return period;
    }

    @Override
    @Transactional
    public SuccessResponseDto periodAssignment(List<UUID> employeeIds) {
        var period = getCurrentActivePeriod();
        if (employeeIds.isEmpty()) {
            throw new NotFoundException("INPUT_REQUIRED", "Provide uuids input for assignment");
        }
        for (var employeeId : employeeIds) {
            handleEmployeePeriodAssignment(employeeId, period);
        }

        return successResponse();
    }

    private void handleEmployeePeriodAssignment(UUID employeeId, Period period) {
        System.out.println("Assign Employee Period");
        var isEmployeePeriodExists = employeePeriodDao.getByEmployeeIdAndPeriodId(employeeId, period.getUuid());
        if (isEmployeePeriodExists != null) {
            updateEmployeePeriodStatus(isEmployeePeriodExists.getUuid(), PeriodStatus.STARTED);
            return;
        }

        var currentMonth = LocalDateTime.now().getMonth();
        var year = period.getStartTime().atZone(ZoneId.systemDefault()).getYear();

        var employeePeriod = saveEmployeePeriod(employeeId, period, year);
        var timelines = generateTimelines(employeePeriod, currentMonth, year);
        timelines.forEach(timeline -> {
            try {
                if (0 == reviewTimelineDao.insert(timeline)) {
                    throw new IntegrityException(TIMELINE_NOT_ASSIGNED.code(), "Timeline not assigned");
                }
            } catch (DataIntegrityViolationException exception) {
                throw new IntegrityException(TIMELINE_NOT_ASSIGNED.code(), exception.getCause().getMessage());
            }
        });
    }

    private EmployeePeriod saveEmployeePeriod(UUID employeeId, Period period, int year) {
        PeriodStatus periodStatus;
        int currentYear = LocalDateTime.now().getYear();
        if (year == currentYear) {
            periodStatus = PeriodStatus.STARTED;
        } else if (year < currentYear) {
            periodStatus = PeriodStatus.COMPLETED;
        } else {
            periodStatus = PeriodStatus.SCHEDULED;
        }
        var employeePeriod = EmployeePeriod.builder()
                .uuid(UUID.randomUUID())
                .employeeUuid(employeeId)
                .periodUuid(period.getUuid())
                .status(periodStatus)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        try {
            if (0 == employeePeriodDao.insert(employeePeriod)) {
                throw new IntegrityException(EMPLOYEE_PERIOD_NOT_CREATED.code(), "Employee Period not created");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_PERIOD_NOT_CREATED.code(), exception.getCause().getMessage());
        }
        return employeePeriod;
    }

    private List<ReviewTimeline> generateTimelines(EmployeePeriod employeePeriod, Month currentMonth, int year) {
        List<ReviewTimeline> timelines = new ArrayList<>();
        int currentYear = LocalDateTime.now().getYear();
        int currentQuarter = (currentMonth.getValue() - 1) / 3 + 1;
        for (int quarter = 1; quarter <= 4; quarter++) {
            ReviewTimelineStatus status;
            if (year < currentYear) {
                status = ReviewTimelineStatus.COMPLETED;
            } else if (year > currentYear || quarter > currentQuarter) {
                status = ReviewTimelineStatus.SCHEDULED;
            } else if (quarter < currentQuarter) {
                status = ReviewTimelineStatus.LOCKED;
            } else {
                status = ReviewTimelineStatus.STARTED;
            }
            var startTime = LocalDateTime.of(year, quarter * 3 - 2, 1, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            var overdueTime = LocalDateTime.of(year, quarter * 3 - 1, 15, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            var lockTime = LocalDateTime.of(year, quarter * 3, 15, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            var endDayOfMonth = YearMonth.of(year, quarter * 3).lengthOfMonth();
            var endTime = LocalDateTime.of(year, quarter * 3, endDayOfMonth, 23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            var timeline = ReviewTimeline.builder()
                    .uuid(UUID.randomUUID())
                    .employeePeriodUuid(employeePeriod.getUuid())
                    .type(ReviewType.valueOf("Q" + quarter))
                    .status(status)
                    .summaryStatus(ReviewStatus.NOT_SUBMITTED)
                    .startTime(startTime)
                    .overdueTime(overdueTime)
                    .lockTime(lockTime)
                    .endTime(endTime)
                    .createdTime(LocalDateTime.now())
                    .updatedTime(LocalDateTime.now())
                    .build();

            timelines.add(timeline);
        }
        return timelines;
    }

    @Override
    @Async
    public void updateEmployeePeriodsByPeriodId(UUID periodId, PeriodStatus status) {
        List<EmployeePeriod> employeePeriods = employeePeriodDao.getByStatusAndPeriodId(PeriodStatus.STARTED, periodId);

        List<CompletableFuture<Void>> futures = employeePeriods.stream()
                .map(employeePeriod -> CompletableFuture.runAsync(() ->
                        updateEmployeePeriodStatus(employeePeriod.getUuid(), status)))
                .toList();

        // Wait for all tasks to complete before returning the response
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        allTasks.thenApply(v -> successResponse());
    }


    @Override
    public SuccessResponseDto updateEmployeePeriodStatus(UUID employeePeriodId, PeriodStatus status) {
        var employeePeriod = getById(employeePeriodId);
        employeePeriod.setStatus(status);

        try {
            if (0 == employeePeriodDao.update(employeePeriod)) {
                throw new IntegrityException(EMPLOYEE_PERIOD_NOT_UPDATED.code(),
                        "Employee Period not updated with id: " + employeePeriod.getUuid());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(EMPLOYEE_PERIOD_NOT_UPDATED.code(),
                    exception.getCause().getMessage());
        }

        var timelines = reviewTimelineDao.getByEmployeePeriodId(employeePeriodId);
        if (status.equals(PeriodStatus.COMPLETED)) {
            timelines.forEach(timeline -> {
                timeline.setStatus(ReviewTimelineStatus.COMPLETED);
                timeline.setUpdatedTime(LocalDateTime.now());
            });
        } else {
            timelines.forEach(timeline -> {
                if (timeline.getStartTime().isAfter(LocalDateTime.now())) {
                    timeline.setStatus(ReviewTimelineStatus.SCHEDULED);
                } else if (timeline.getStartTime().isBefore(LocalDateTime.now()) && timeline.getOverdueTime().isAfter(LocalDateTime.now())) {
                    timeline.setStatus(ReviewTimelineStatus.STARTED);
                } else if (timeline.getOverdueTime().isBefore(LocalDateTime.now()) && timeline.getLockTime().isAfter(LocalDateTime.now())) {
                    timeline.setStatus(ReviewTimelineStatus.OVERDUE);
                } else if (timeline.getLockTime().isBefore(LocalDateTime.now()) && timeline.getEndTime().isAfter(LocalDateTime.now())) {
                    timeline.setStatus(ReviewTimelineStatus.LOCKED);
                } else {
                    timeline.setStatus(ReviewTimelineStatus.COMPLETED);
                }

                timeline.setUpdatedTime(LocalDateTime.now());
            });
            timelines.forEach(timeline -> {
                try {
                    if (0 == reviewTimelineDao.update(timeline)) {
                        throw new IntegrityException(TIMELINE_NOT_UPDATED.code(),
                                "Timeline not updated for id: " + timeline.getUuid());
                    }
                } catch (DataIntegrityViolationException exception) {
                    throw new IntegrityException(TIMELINE_NOT_UPDATED.code(),
                            exception.getCause().getMessage());
                }
            });
        }
        return successResponse();
    }


    @Override
    public FullEmployeePeriodDto getEmployeePeriodById(UUID employeePeriodId) {
        var employeePeriod = getById(employeePeriodId);
        var fullEmployeePeriod = employeePeriodMapper
                .employeePeriodToFullEmployeePeriodDto(employeePeriod);
        var timelines = reviewTimelineDao.getByEmployeePeriodId(employeePeriodId);
        fullEmployeePeriod.setReviewTimelines(timelines);
        return fullEmployeePeriod;
    }

    @Override
    public EmployeeCycleAndTimelineResponseDto getByEmployeeIdAndPeriodId(UUID employeeId, UUID periodId) {
        var period = getPeriod(periodId);
        var employeePeriod = employeePeriodDao.getByEmployeeIdAndPeriodId(employeeId,
                periodId);
        if (employeePeriod == null) {
            throw new NotFoundException("EMPLOYEE_PERIOD_NOT_FOUND", "Employee period not found for employee");
        }

        return toEmployeeCycleAndTimelineResponseDtoMap(employeeId, employeePeriod, period);
    }

    @Override
    public Map<String, EmployeeCycleAndTimelineResponseDto> getAll(UUID employeeId) {
        var employeePeriods = employeePeriodDao.getByEmployeeId(employeeId);
        if (employeePeriods == null) {
            throw new NotFoundException("PERIOD_NOT_EXISTS", "Employee is not assigned with period");
        }
        employeePeriods = employeePeriods.stream()
                .filter(employeePeriod -> !employeePeriod.getStatus().equals(PeriodStatus.INACTIVE))
                .toList();
        Map<String, EmployeeCycleAndTimelineResponseDto> responseDtoMap = new HashMap<>();
        for (var employeePeriod : employeePeriods) {
            var period = getPeriod(employeePeriod.getPeriodUuid());
            var employeeCycleAndTimelineResponseDto =
                    toEmployeeCycleAndTimelineResponseDtoMap(employeeId, employeePeriod, period);
            responseDtoMap.put(period.getName(), employeeCycleAndTimelineResponseDto);
        }
        return responseDtoMap;
    }

    private EmployeeCycleAndTimelineResponseDto toEmployeeCycleAndTimelineResponseDtoMap(UUID employeeId, EmployeePeriod employeePeriod, Period period) {
        return EmployeeCycleAndTimelineResponseDto.builder()
                .employeeId(employeeId)
                .employeeCycleId(employeePeriod.getUuid())
                .period(period)
                .Q1(reviewTimelineService.getByEmployeePeriodIdAndReviewType(employeePeriod.getUuid(), ReviewType.Q1))
                .Q2(reviewTimelineService.getByEmployeePeriodIdAndReviewType(employeePeriod.getUuid(), ReviewType.Q2))
                .Q3(reviewTimelineService.getByEmployeePeriodIdAndReviewType(employeePeriod.getUuid(), ReviewType.Q3))
                .Q4(reviewTimelineService.getByEmployeePeriodIdAndReviewType(employeePeriod.getUuid(), ReviewType.Q4))
                .build();
    }

    @Override
    public EmployeeCycleAndTimelineResponseDto getByYear(UUID employeeId, Optional<Long> optionalYear) {
        var year = optionalYear.isEmpty() ? LocalDateTime.now().getYear() : optionalYear.get();
        var period = getPeriodByYear(year);
        var employeePeriod = employeePeriodDao.getByEmployeeIdAndPeriodId(employeeId, period.getUuid());
        if (employeePeriod == null) {
            throw new NotFoundException("EMPLOYEE_PERIOD_NOT_FOUND", "Employee period not found for employee: " + employeeId);
        }

        return toEmployeeCycleAndTimelineResponseDtoMap(employeeId, employeePeriod, period);
    }

    @Override
    public List<Integer> getAllEligibleYears(UUID employeeId) {
        var employeePeriods = employeePeriodDao.getByEmployeeIdAndStatus(employeeId, List.of(PeriodStatus.STARTED, PeriodStatus.COMPLETED));

        return employeePeriods.stream()
                .map(EmployeePeriod::getPeriodUuid)
                .distinct()
                .map(periodDao::getById)
                .filter(Objects::nonNull)
                .map(Period::getName)
                .map(Integer::parseInt)
                .sorted(Collections.reverseOrder())
                .toList();
    }


    private EmployeePeriod getById(UUID employeePeriodId) {
        var employeePeriod = employeePeriodDao.getById(employeePeriodId);
        if (employeePeriod == null) {
            throw new NotFoundException("", "Employee Period not found with id " + employeePeriodId);
        }
        return employeePeriod;
    }

    private SuccessResponseDto successResponse() {
        return SuccessResponseDto.builder()
                .success(Boolean.TRUE)
                .data(String.valueOf(UUID.randomUUID()))
                .build();
    }

    private Period getPeriod(UUID periodId) {
        var period = periodDao.getById(periodId);
        if (period == null) {
            throw new NotFoundException(PERIOD_NOT_EXISTS.code(), "Period not found");
        }
        return period;
    }

    private Period getPeriodByYear(long year) {
        var period = periodDao.getByYear(year);
        if (period == null) {
            throw new NotFoundException(PERIOD_NOT_EXISTS.code(), "Period not exists");
        }
        return period;
    }
}