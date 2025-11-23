package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.ReviewDao;
import com.learning.emsmybatisliquibase.dao.ReviewTimelineDao;
import com.learning.emsmybatisliquibase.entity.Review;
import com.learning.emsmybatisliquibase.entity.enums.ReviewStatus;
import com.learning.emsmybatisliquibase.entity.ReviewTimeline;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.ReviewService;
import com.learning.emsmybatisliquibase.service.ReviewTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.ReviewErrorCodes.REVIEW_NOT_EXISTS;
import static com.learning.emsmybatisliquibase.exception.errorcodes.ReviewErrorCodes.REVIEW_NOT_DELETED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.ReviewErrorCodes.REVIEW_NOT_CREATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.ReviewErrorCodes.REVIEW_NOT_UPDATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.ReviewErrorCodes.REVIEW_ALREADY_EXISTS;

import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_NOT_UPDATED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_NOT_STARTED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_COMPLETED;
import static com.learning.emsmybatisliquibase.exception.errorcodes.TimelineErrorCodes.TIMELINE_LOCKED;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewTimelineService reviewTimelineService;

    private final ReviewTimelineDao reviewTimelineDao;

    private final ReviewDao reviewDao;

    private final EmployeeService employeeService;

    @Override
    @Transactional
    public Review add(UUID employeeUuid, Review reviewDto) {
        var timeline = reviewTimelineService.getById(reviewDto.getTimelineUuid());
        if (timeline == null) {
            throw new NotFoundException("TIMELINE_NOT_FOUND", "Timeline not found with id: " + reviewDto.getTimelineUuid());
        }

        var isReviewExists = reviewDao.getByTimelineId(reviewDto.getTimelineUuid());
        if (isReviewExists != null) {
            throw new FoundException(REVIEW_ALREADY_EXISTS.code(), "Review already exists");
        }

        validateTimeline(employeeUuid, timeline);

        var review = new Review();
        review.setUuid(UUID.randomUUID());
        setReview(review, reviewDto, employeeUuid);

        try {
            if (0 == reviewDao.insert(review)) {
                throw new IntegrityException(REVIEW_NOT_CREATED.code(),
                        "Review is not inserted with Id: " + review.getUuid());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(REVIEW_NOT_CREATED.code(),
                    exception.getCause().getMessage());
        }
        timeline.setSummaryStatus(review.getStatus());
        updateTimeline(timeline);

        return review;
    }

    private void updateTimeline(ReviewTimeline timeline) {
        try {
            if (0 == reviewTimelineDao.update(timeline)) {
                throw new IntegrityException(TIMELINE_NOT_UPDATED.code(),
                        "Timeline not updated for uuid: " + timeline.getUuid());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(TIMELINE_NOT_UPDATED.code(),
                    exception.getCause().getMessage());
        }
    }

    @Override
    public Review update(UUID employeeUuid, UUID reviewUuid, Review reviewDto) {
        var timeline = reviewTimelineService.getById(reviewDto.getTimelineUuid());

        var review = reviewDao.getByTimelineId(reviewDto.getTimelineUuid());

        if (review == null) {
            throw new NotFoundException(REVIEW_NOT_EXISTS.code(),
                    "Review not found with Id: " + reviewUuid);
        }

        validateTimeline(employeeUuid, timeline);

        setReview(review, reviewDto, employeeUuid);

        try {
            if (0 == reviewDao.update(review)) {
                throw new IntegrityException(REVIEW_NOT_UPDATED.code(),
                        "Review not updated for Id: " + reviewUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(REVIEW_NOT_UPDATED.code(),
                    exception.getCause().getMessage());
        }
        return review;
    }

    @Override
    public Review getById(UUID reviewUuid) {
        var review = reviewDao.getById(reviewUuid);
        if (review == null) {
            throw new NotFoundException(REVIEW_NOT_EXISTS.code(),
                    "Review not found with Id: " + reviewUuid);
        }
        return review;
    }

    @Override
    public void deleteById(UUID reviewUuid) {
        getById(reviewUuid);
        try {
            if (0 == reviewDao.delete(reviewUuid)) {
                throw new IntegrityException(REVIEW_NOT_DELETED.code(),
                        "Review not deleted with Id: " + reviewUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException(REVIEW_NOT_DELETED.code(),
                    exception.getCause().getMessage());
        }
        var isReviewExists = reviewDao.getById(reviewUuid);
        if (isReviewExists != null) {
            throw new FoundException(REVIEW_NOT_DELETED.code(),
                    "Review not deleted with Id: " + reviewUuid);
        }
    }

    private void setReview(Review existingReview, Review updatedReview, UUID employeeUuid) {
        var currentUser = getCurrentUser();
        var employee = employeeService.getById(employeeUuid);

        if (currentUser.equals(employeeUuid)) {
            existingReview.setStatus(ReviewStatus.WAITING_FOR_APPROVAL);
            setEmployeeReview(existingReview, updatedReview);
        } else if (currentUser.equals(employee.getManagerUuid())) {
            setManagerReview(existingReview, updatedReview);
            existingReview.setStatus(ReviewStatus.APPROVED);
        }
    }

    private void setEmployeeReview(Review existingReview, Review updatedReview) {
        existingReview.setTimelineUuid(updatedReview.getTimelineUuid());
        existingReview.setWhatWentWell(updatedReview.getWhatWentWell());
        existingReview.setWhatDoneBetter(updatedReview.getWhatDoneBetter());
        existingReview.setWayForward(updatedReview.getWayForward());
        existingReview.setOverallComments(updatedReview.getOverallComments());
        existingReview.setType(updatedReview.getType());
        existingReview.setUpdatedTime(LocalDateTime.now());
    }

    private void setManagerReview(Review existingReview, Review updatedReview) {
        existingReview.setTimelineUuid(updatedReview.getTimelineUuid());
        existingReview.setWhatWentWell(updatedReview.getWhatWentWell());
        existingReview.setWhatDoneBetter(updatedReview.getWhatDoneBetter());
        existingReview.setWayForward(updatedReview.getWayForward());
        existingReview.setOverallComments(updatedReview.getOverallComments());
        existingReview.setType(updatedReview.getType());
        existingReview.setManagerWhatWentWell(updatedReview.getManagerWhatWentWell());
        existingReview.setManagerWhatDoneBetter(updatedReview.getManagerWhatDoneBetter());
        existingReview.setManagerWayForward(updatedReview.getManagerWayForward());
        existingReview.setManagerOverallComments(updatedReview.getManagerOverallComments());
        existingReview.setRating(updatedReview.getRating());
        existingReview.setUpdatedTime(LocalDateTime.now());
    }

    private void validateTimeline(UUID employeeUuid, ReviewTimeline reviewTimeline) {
        var currentUser = getCurrentUser();
        var employee = employeeService.getById(employeeUuid);

        if (!currentUser.equals(employeeUuid) && !currentUser.equals(employee.getManagerUuid())) {
            throw new IntegrityException("INVALID_USER", "Invalid User trying to insert data");
        }

        switch (reviewTimeline.getStatus()) {
            case COMPLETED:
                throw new InvalidInputException(TIMELINE_COMPLETED.code(), "Review already completed");
            case LOCKED:
                throw new InvalidInputException(TIMELINE_LOCKED.code(), "Review already locked");
            case NOT_STARTED:
                throw new InvalidInputException(TIMELINE_NOT_STARTED.code(), "Review not started");
            default:
                break;
        }
    }

    private UUID getCurrentUser() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}