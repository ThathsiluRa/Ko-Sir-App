package com.tutorplatform.repository;

import com.tutorplatform.model.Booking;
import com.tutorplatform.model.Review;
import com.tutorplatform.model.TutorProfile;
import com.tutorplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * REVIEW REPOSITORY
 * =====================================================
 * Handles database operations for Review.
 *
 * Key use cases:
 * - Display all reviews on a tutor's profile
 * - Check if a student already reviewed a booking
 * - Admin can view and delete any review
 * =====================================================
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Get all reviews for a specific tutor.
     * Ordered by newest first.
     * Displayed on the tutor's public profile page.
     */
    List<Review> findByTutorProfileOrderByCreatedAtDesc(TutorProfile tutorProfile);

    /**
     * Check if a review already exists for a specific booking.
     * Prevents students from reviewing the same session twice.
     */
    Optional<Review> findByBooking(Booking booking);

    /**
     * Get all reviews written by a specific student.
     * Displayed on the student's review history page.
     */
    List<Review> findByStudentOrderByCreatedAtDesc(User student);

    /**
     * AVERAGE RATING QUERY
     * Calculates the average star rating for a tutor across all their reviews.
     * Returns null if the tutor has no reviews yet.
     *
     * This is used to verify/sync the averageRating stored on TutorProfile.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tutorProfile = :tutorProfile")
    Double calculateAverageRating(@Param("tutorProfile") TutorProfile tutorProfile);

    /**
     * Count all reviews on the platform.
     * Used for admin dashboard stats.
     */
    long count();

    /**
     * Count reviews for a specific tutor.
     */
    long countByTutorProfile(TutorProfile tutorProfile);
}
