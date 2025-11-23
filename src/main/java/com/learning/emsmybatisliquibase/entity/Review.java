package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.ReviewRating;
import com.learning.emsmybatisliquibase.entity.enums.ReviewStatus;
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
public class Review implements Serializable {

    private UUID uuid;

    private UUID timelineUuid;

    private ReviewType type;

    private String whatWentWell;

    private String whatDoneBetter;

    private String wayForward;

    private String overallComments;

    private String managerWhatWentWell;

    private String managerWhatDoneBetter;

    private String managerWayForward;

    private String managerOverallComments;

    private ReviewRating rating;

    private ReviewStatus status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
