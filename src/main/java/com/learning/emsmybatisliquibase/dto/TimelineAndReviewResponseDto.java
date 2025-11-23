package com.learning.emsmybatisliquibase.dto;

import com.learning.emsmybatisliquibase.entity.Review;
import com.learning.emsmybatisliquibase.entity.enums.ReviewStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimelineAndReviewResponseDto {
    private UUID uuid;

    private UUID employeePeriodUuid;

    private ReviewType type;

    private LocalDateTime startTime;

    private LocalDateTime overdueTime;

    private LocalDateTime lockTime;

    private LocalDateTime endTime;

    private ReviewTimelineStatus status;

    private ReviewStatus summaryStatus;

    private Review review;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
