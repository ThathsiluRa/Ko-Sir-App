package com.tutorplatform.service;

import com.tutorplatform.model.*;
import com.tutorplatform.repository.ReviewRepository;
import com.tutorplatform.repository.TutorProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * REVIEW SERVICE
 * =====================================================
 * Business logic for the platform review system.
 *
 * Rules:
 * - Only one review per completed booking
 * - Only the student who booked can review
 * - Reviews update the tutor's average rating automatically
 * - Admin can delete any review
 * =====================================================
 */
@Service
@Transactional
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    /**
     * We need this to update the tutor's average rating
     * after a new review is submitted.
     */
    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    // =====================================================
    // SUBMIT REVIEW (student action)
    // =====================================================

    /**
     * Submit a review for a completed booking.
     *
     * VALIDATION:
     * 1. Booking must be COMPLETED
     * 2. No existing review for this booking
     * 3. The reviewer must be the student who booked
     *
     * AFTER SAVING:
     * - Updates the tutor's averageRating and totalReviews automatically
     *
     * @param booking    the completed booking being reviewed
     * @param student    the student submitting the review
     * @param rating     star rating (1-5)
     * @param comment    optional written feedback
     */
    public Review submitReview(Booking booking, User student, int rating, String comment) {

        // Validate booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("You can only review completed sessions");
        }

        // Check no review already exists for this booking
        if (reviewRepository.findByBooking(booking).isPresent()) {
            throw new IllegalStateException("You have already reviewed this session");
        }

        // Validate the reviewer is the student who made the booking
        if (!booking.getStudentProfile().getUser().getId().equals(student.getId())) {
            throw new SecurityException("You can only review your own sessions");
        }

        // Create and save the review
        Review review = new Review(booking, booking.getTutorProfile(), student, rating, comment);
        Review savedReview = reviewRepository.save(review);

        // Update the tutor's average rating
        updateTutorRating(booking.getTutorProfile(), rating);

        return savedReview;
    }

    /**
     * Updates the tutor profile's averageRating and totalReviews.
     * Called after every new review is submitted.
     *
     * Uses the addReviewRating() helper method defined in TutorProfile.
     */
    private void updateTutorRating(TutorProfile tutorProfile, int newRating) {
        tutorProfile.addReviewRating(newRating);
        tutorProfileRepository.save(tutorProfile);
    }

    // =====================================================
    // QUERY METHODS
    // =====================================================

    /** Get all reviews for a tutor (displayed on their profile) */
    @Transactional(readOnly = true)
    public List<Review> getReviewsForTutor(TutorProfile tutorProfile) {
        return reviewRepository.findByTutorProfileOrderByCreatedAtDesc(tutorProfile);
    }

    /** Get all reviews written by a student */
    @Transactional(readOnly = true)
    public List<Review> getReviewsByStudent(User student) {
        return reviewRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    /** Check if a booking has been reviewed */
    @Transactional(readOnly = true)
    public boolean hasReview(Booking booking) {
        return reviewRepository.findByBooking(booking).isPresent();
    }

    /** Admin: get all reviews on the platform */
    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    /** Admin: get total review count for stats */
    @Transactional(readOnly = true)
    public long countAllReviews() {
        return reviewRepository.count();
    }

    // =====================================================
    // ADMIN OPERATIONS
    // =====================================================

    /**
     * Admin deletes a review.
     * This should also recalculate the tutor's average rating,
     * but for simplicity we just remove the review here.
     * In production, you'd recalculate from all remaining reviews.
     */
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        TutorProfile tutor = review.getTutorProfile();

        // Remove the review
        reviewRepository.delete(review);

        // Recalculate average from remaining reviews
        Double newAverage = reviewRepository.calculateAverageRating(tutor);
        long newCount = reviewRepository.countByTutorProfile(tutor);

        tutor.setAverageRating(newAverage != null ? Math.round(newAverage * 10.0) / 10.0 : 0.0);
        tutor.setTotalReviews((int) newCount);
        tutorProfileRepository.save(tutor);
    }
}
