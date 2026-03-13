package com.tutorplatform.repository;

import com.tutorplatform.model.TutorProfile;
import com.tutorplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TUTOR PROFILE REPOSITORY
 * =====================================================
 * Handles all database operations for TutorProfile.
 *
 * Key queries:
 * - Find a tutor's profile from their User account
 * - Search tutors by subject (for student search feature)
 * - Get all approved tutors (only these show in search results)
 * =====================================================
 */
@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, Long> {

    /**
     * Find a tutor's profile by their User account.
     * Used when a logged-in tutor wants to see their own profile.
     * Example: tutorProfileRepo.findByUser(currentUser)
     */
    Optional<TutorProfile> findByUser(User user);

    /**
     * Find all approved tutors.
     * Only approved tutors appear in student search results.
     * Admin must approve new tutors before they become visible.
     */
    List<TutorProfile> findByApprovedTrue();

    /**
     * Find all approved tutors who are accepting bookings.
     * This is what students see when searching for available tutors.
     */
    List<TutorProfile> findByApprovedTrueAndAcceptingBookingsTrue();

    /**
     * CUSTOM JPQL SEARCH QUERY
     * Search for approved tutors by subject keyword.
     *
     * @Query uses JPQL (Java Persistence Query Language) - like SQL but
     * uses entity class names and field names, not table/column names.
     *
     * The query: find tutors whose "subjects" field contains the search term.
     * LOWER() makes the search case-insensitive.
     * :subject is a named parameter (bound via @Param).
     *
     * Example: searchBySubject("math") finds tutors with "Mathematics",
     * "Math", "Maths" etc. in their subjects field.
     */
    @Query("SELECT t FROM TutorProfile t WHERE t.approved = true " +
           "AND t.acceptingBookings = true " +
           "AND LOWER(t.subjects) LIKE LOWER(CONCAT('%', :subject, '%'))")
    List<TutorProfile> searchBySubject(@Param("subject") String subject);

    /**
     * SEARCH BY SUBJECT OR LOCATION
     * More flexible search - students can find tutors by what they teach
     * AND where they are located.
     */
    @Query("SELECT t FROM TutorProfile t WHERE t.approved = true " +
           "AND t.acceptingBookings = true " +
           "AND (LOWER(t.subjects) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.location) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<TutorProfile> searchBySubjectOrLocation(@Param("keyword") String keyword);

    /**
     * Find all tutors pending approval.
     * Admin uses this to review new tutor registrations.
     */
    List<TutorProfile> findByApprovedFalse();

    /**
     * Check if a user already has a tutor profile.
     * Prevents creating duplicate profiles.
     */
    boolean existsByUser(User user);

    /**
     * Count all approved tutors.
     * Used for admin dashboard statistics.
     */
    long countByApprovedTrue();
}
