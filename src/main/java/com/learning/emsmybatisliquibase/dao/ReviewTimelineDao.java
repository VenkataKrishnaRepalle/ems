package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.dto.NotificationDto;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import com.learning.emsmybatisliquibase.entity.ReviewTimeline;
import com.learning.emsmybatisliquibase.entity.enums.ReviewTimelineStatus;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewTimelineDao {

    List<ReviewTimeline> getByEmployeePeriodId(@Param("employeePeriodUuid") UUID employeePeriodUuid);

    int insert(@Param("reviewTimeline") ReviewTimeline reviewTimeline);

    int update(@Param("reviewTimeline") ReviewTimeline reviewTimeline);

    List<ReviewTimeline> findByStatusAndReviewType(@Param("status") PeriodStatus status, @Param("reviewType") ReviewType reviewType);

    ReviewTimeline getById(@Param("uuid") UUID uuid);

    List<ReviewTimeline> getByEmployeeUuidsAndReviewType(@Param("employeeUuids") List<UUID> employeeUuids, @Param("reviewType") ReviewType reviewType);

    List<NotificationDto> getTimelineIdsByReviewType(@Param("reviewType") ReviewType reviewType);

    ReviewTimeline getByEmployeePeriodIdAndReviewType(@Param("employeePeriodId") UUID employeePeriodId, @Param("reviewType") ReviewType reviewType);

    List<UUID> getAllTimelineUuidsByReviewType(@Param("periodUuid") UUID periodUuid, @Param("reviewType") ReviewType reviewType);

    int updateStatus(@Param("periodUuid") UUID periodUuid, @Param("reviewType") ReviewType reviewType,
                      @Param("reviewStatus") ReviewTimelineStatus reviewTimelineStatus);
}
