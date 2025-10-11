package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.dto.FullEmployeePeriodDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.dto.TimelineAndReviewResponseDto;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.service.ReviewTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/timeline")
@RequiredArgsConstructor
public class ReviewTimelineController {

    private final ReviewTimelineService reviewTimelineService;

    @GetMapping("/getActiveTimelineDetails/{employeeId}")
    public ResponseEntity<FullEmployeePeriodDto> getActiveTimelineDetails(@PathVariable UUID employeeId) {
        return new ResponseEntity<>(reviewTimelineService.getActiveTimelineDetails(employeeId),
                HttpStatus.OK);
    }

    @PutMapping("/updateTimelineStatus")
    public ResponseEntity<SuccessResponseDto> updateTimelineStatus(@RequestBody List<UUID> employeeUuids,
                                                                   @RequestBody ReviewType reviewType,
                                                                   @RequestBody ReviewTimelineStatus reviewTimelineStatus) {
        reviewTimelineService.updateTimelineStatus(employeeUuids, reviewType, reviewTimelineStatus);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PutMapping("/startTimeline")
    public ResponseEntity<SuccessResponseDto> updateTimelineForQuarter(@RequestParam ReviewType completedReviewType,
                                                                       @RequestParam ReviewType startReviewType) {
        return new ResponseEntity<>(reviewTimelineService.startTimelinesForQuarter(completedReviewType,
                startReviewType),
                HttpStatus.ACCEPTED);
    }

    @GetMapping("/getByEmployeePeriodId/{employeePeriodId}/type/{reviewType}")
    public ResponseEntity<TimelineAndReviewResponseDto> getByEmployeePeriodIdAndReviewType(@PathVariable UUID employeePeriodId, @PathVariable ReviewType reviewType) {
        return new ResponseEntity<>(reviewTimelineService.getByEmployeePeriodAndReviewType(employeePeriodId, reviewType),
                HttpStatus.OK);
    }
}
