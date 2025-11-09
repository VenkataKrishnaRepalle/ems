package com.learning.emsmybatisliquibase.scheduled;

import com.learning.emsmybatisliquibase.dao.*;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.*;
import com.learning.emsmybatisliquibase.entity.enums.*;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.service.*;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;

@Service
@AllArgsConstructor
public class ScheduledTasks {

    private final EmployeeDao employeeDao;

    private final ProfileDao profileDao;

    private final PeriodDao periodDao;

    private final ReviewTimelineService reviewTimelineService;

    private final EmployeePeriodDao employeePeriodDao;

    private final EmployeePeriodService employeePeriodService;

    private final PeriodService periodService;

    private final CommunicationService communicationService;

    private final ReviewTimelineDao reviewTimelineDao;

    private final OtpDao otpDao;

    private final OtpService otpService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void updateProfileStatusLeavingDate() {
        List<Employee> employees = employeeDao.getActiveEmployeesWithPastLeavingDate();
        if (employees.isEmpty()) {
            return;
        }
        for (Employee employee : employees) {
            var profile = profileDao.get(employee.getUuid());
            if (employee.getLeavingDate() != null && profile.getProfileStatus() != ProfileStatus.INACTIVE) {
                profile.setProfileStatus(ProfileStatus.INACTIVE);
                profile.setUpdatedTime(Instant.now());
                try {
                    if (0 == profileDao.update(profile)) {
                        throw new IntegrityException("PROFILE_UPDATE_FAILED", "Profile not updated for uuid: " + profile.getEmployeeUuid());
                    }
                } catch (DataIntegrityViolationException exception) {
                    throw new IntegrityException("PROFILE_UPDATE_FAILED", exception.getMessage());
                }

                var employeeCycles = employeePeriodDao.getByEmployeeIdAndStatus(
                        employee.getUuid(), List.of(PeriodStatus.STARTED));
                employeeCycles.forEach(employeeCycle ->
                        employeePeriodService.updateEmployeePeriodStatus(employeeCycle.getUuid(),
                                PeriodStatus.INACTIVE));
            }
        }
    }

    @Scheduled(cron = "0 0 0 25 12 *")
    public void schedulePeriod() {
        var year = Instant.now().atZone(ZoneId.systemDefault()).getYear() + 1;
        periodService.createPeriod(year);
    }

    @Scheduled(cron = "0 14 19 13 2 *")
    public void stopOldPeriod() {
        var oldPeriod = periodDao.getByStatus(PeriodStatus.STARTED);
        periodService.updateStatus(oldPeriod.getUuid(), PeriodStatus.COMPLETED);
    }

    @Scheduled(cron = "0 0 0 1 1 *")
    public void startPeriod1() {
        var oldPeriod = periodDao.getByStatus(PeriodStatus.STARTED);
        oldPeriod.setStatus(PeriodStatus.INACTIVE);
        oldPeriod.setUpdatedTime(Instant.now());

        var employeePeriods = employeePeriodDao.getByStatusAndPeriodId(PeriodStatus.STARTED,
                oldPeriod.getUuid());
        employeePeriods.forEach(employeePeriod ->
                employeePeriodService.updateEmployeePeriodStatus(employeePeriod.getUuid(),
                        PeriodStatus.COMPLETED));

        var period = periodDao.getByStatus(PeriodStatus.SCHEDULED);
        if (period != null) {
            period.setStatus(PeriodStatus.STARTED);
            period.setUpdatedTime(Instant.now());
            try {
                if (0 == periodDao.update(period)) {
                    throw new IntegrityException("PERIOD_NOT_UPDATED", "Period not updated");
                }
            } catch (DataIntegrityViolationException exception) {
                throw new IntegrityException("PERIOD_NOT_UPDATED", exception.getCause().getMessage());
            }
        }

        employeePeriodService.periodAssignment(employeeDao.getAllActiveEmployeeIds(
                List.of(ProfileStatus.ACTIVE, ProfileStatus.PENDING)));
    }

    @Scheduled(cron = "0 15 0 1 4,7,10 *")
    public void startTimeline() {
        var calender = Calendar.getInstance();
        var month = calender.get(Calendar.MONTH);

        ReviewType completedReviewType = null;
        ReviewType startedReviewType = null;

        switch (month) {
            case Calendar.APRIL:
                completedReviewType = ReviewType.Q1;
                startedReviewType = ReviewType.Q2;
                break;
            case Calendar.JULY:
                completedReviewType = ReviewType.Q2;
                startedReviewType = ReviewType.Q3;
                break;
            case Calendar.OCTOBER:
                completedReviewType = ReviewType.Q3;
                startedReviewType = ReviewType.Q4;
                break;
            default:
                break;
        }
        if (completedReviewType != null) {
            reviewTimelineService.startTimelinesForQuarter(completedReviewType, startedReviewType);
        }
    }

    @Scheduled(cron = "0 0 0 25 3,6,9,12 *")
    public void sendBeforeStartNotification() {
        var calender = Calendar.getInstance();
        var month = calender.get(Calendar.MONTH);

        ReviewType reviewType = null;
        switch (month + 1) {
            case Calendar.APRIL:
                reviewType = ReviewType.Q2;
                break;
            case Calendar.JULY:
                reviewType = ReviewType.Q3;
                break;
            case Calendar.OCTOBER:
                reviewType = ReviewType.Q4;
                break;
            default:
                break;
        }

        if (reviewType != null) {
            var notifications = reviewTimelineDao.getTimelineIdsByReviewType(reviewType);
            communicationService.sendNotificationBeforeStart(notifications, reviewType);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteOldOtp() {
        var requestQuery = new RequestQuery();
        requestQuery.setProperty("types", List.of(OtpAuthType.values()));
        requestQuery.setProperty("statuses", List.of(OtpAuthStatus.values()));

        var existingOtpList = otpDao.get(requestQuery);
        int count = 0;
        for (var otp : existingOtpList) {
            otpService.delete(otp.getUuid());
            count++;
            if (count % 100 == 0) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}