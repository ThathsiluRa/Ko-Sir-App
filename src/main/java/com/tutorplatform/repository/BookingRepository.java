package com.tutorplatform.repository;

import com.tutorplatform.model.Booking;
import com.tutorplatform.model.BookingStatus;
import com.tutorplatform.model.StudentProfile;
import com.tutorplatform.model.TutorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * BOOKING REPOSITORY
 * =====================================================
 * Handles all database operations for Booking.
 *
 * This is the most query-heavy repository because:
 * - Students need to see their own bookings
 * - Tutors need to see bookings assigned to them
 * - Admin needs to see all bookings, filtered by various criteria
 * =====================================================
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // =====================================================
    // STUDENT QUERIES - used in student dashboard
    // =====================================================

    /**
     * Get all bookings for a specific student.
     * Ordered by date descending (newest first).
     */
    List<Booking> findByStudentProfileOrderByCreatedAtDesc(StudentProfile studentProfile);

    /**
     * Get a student's bookings filtered by status.
     * Example: all PENDING bookings for a student.
     */
    List<Booking> findByStudentProfileAndStatus(StudentProfile studentProfile, BookingStatus status);

    // =====================================================
    // TUTOR QUERIES - used in tutor dashboard
    // =====================================================

    /**
     * Get all bookings for a specific tutor.
     * Ordered newest first.
     */
    List<Booking> findByTutorProfileOrderByCreatedAtDesc(TutorProfile tutorProfile);

    /**
     * Get a tutor's bookings filtered by status.
     * Example: all PENDING bookings that need the tutor's response.
     */
    List<Booking> findByTutorProfileAndStatus(TutorProfile tutorProfile, BookingStatus status);

    /**
     * Get confirmed bookings for a tutor on a specific date.
     * Used to check for scheduling conflicts before accepting a new booking.
     */
    List<Booking> findByTutorProfileAndBookingDateAndStatus(
            TutorProfile tutorProfile,
            LocalDate bookingDate,
            BookingStatus status
    );

    // =====================================================
    // ADMIN QUERIES - used in admin panel
    // =====================================================

    /**
     * Get all bookings filtered by status.
     * Admin can filter the bookings list by PENDING, CONFIRMED, etc.
     */
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    /**
     * Count bookings by status.
     * Used for the admin dashboard statistics cards.
     * Example: countByStatus(BookingStatus.PENDING) → number of pending bookings
     */
    long countByStatus(BookingStatus status);

    /**
     * CUSTOM STATS QUERY
     * Count total completed bookings for the admin dashboard.
     * This represents the platform's "total sessions delivered".
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'COMPLETED'")
    long countCompletedBookings();

    /**
     * Count total bookings made today.
     * Shows platform activity on the admin dashboard.
     */
    long countByBookingDate(LocalDate date);
}
