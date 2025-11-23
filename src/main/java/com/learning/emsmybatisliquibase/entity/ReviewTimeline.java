package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.ReviewStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewTimeline implements Serializable {

    private UUID uuid;

    private UUID employeePeriodUuid;

    private ReviewType type;

    private LocalDateTime startTime;

    private LocalDateTime overdueTime;

    private LocalDateTime lockTime;

    private LocalDateTime endTime;

    private ReviewTimelineStatus status;

    private ReviewStatus summaryStatus;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
