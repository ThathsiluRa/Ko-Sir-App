package com.tutorplatform.service;

import com.tutorplatform.model.*;
import com.tutorplatform.repository.BookingRepository;
import com.tutorplatform.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * BOOKING SERVICE
 * =====================================================
 * All business logic for managing tutoring session bookings.
 *
 * This service handles:
 * - Creating new bookings (student action)
 * - Confirming/rejecting bookings (tutor action)
 * - Completing and cancelling bookings
 * - Admin: full booking management
 * =====================================================
 */
@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    // =====================================================
    // CREATE BOOKING (student action)
    // =====================================================

    /**
     * Create a new booking when a student requests a tutoring session.
     *
     * STEPS:
     * 1. Get the student's profile
     * 2. Calculate the total price
     * 3. Save the booking with PENDING status
     *
     * @param studentUser     the logged-in student
     * @param tutorProfile    the tutor being booked
     * @param subject         subject for the session
     * @param date            session date
     * @param time            session start time
     * @param duration        session length in hours
     * @param sessionLocation where the session takes place (student's home address
     *                        or tutor's fixed teaching address)
     * @param notes           optional notes from student
     */
    public Booking createBooking(User studentUser, TutorProfile tutorProfile,
                                  String subject, LocalDate date, LocalTime time,
                                  int duration, String sessionLocation, String notes) {

        // Get the student's profile (need it for the FK relationship)
        StudentProfile studentProfile = studentProfileRepository.findByUser(studentUser)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));

        // Calculate total price: hourly rate × number of hours
        BigDecimal totalPrice = tutorProfile.getHourlyRate()
                .multiply(BigDecimal.valueOf(duration));

        // Build the Booking entity
        Booking booking = new Booking();
        booking.setStudentProfile(studentProfile);
        booking.setTutorProfile(tutorProfile);
        booking.setSubject(subject);
        booking.setBookingDate(date);
        booking.setStartTime(time);
        booking.setDurationHours(duration);
        booking.setTotalPrice(totalPrice);
        booking.setSessionLocation(sessionLocation);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.PENDING_PAYMENT); // Waits for payment before tutor sees it

        return bookingRepository.save(booking);
    }

    // =====================================================
    // STATUS UPDATES (tutor actions)
    // =====================================================

    /**
     * Tutor confirms a pending booking.
     * The session is now scheduled - both parties should be informed.
     *
     * @param bookingId  the booking to confirm
     * @param tutorUser  the logged-in tutor (for authorization check)
     */
    public Booking confirmBooking(Long bookingId, User tutorUser) {
        Booking booking = findById(bookingId);

        // Security check: ensure the tutor owns this booking
        if (!booking.getTutorProfile().getUser().getId().equals(tutorUser.getId())) {
            throw new SecurityException("You are not authorized to confirm this booking");
        }

        // Can only confirm PENDING bookings
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    /**
     * Tutor rejects a booking request.
     *
     * @param bookingId  the booking to reject
     * @param tutorUser  the logged-in tutor
     * @param reason     optional reason for rejection
     */
    public Booking rejectBooking(Long bookingId, User tutorUser, String reason) {
        Booking booking = findById(bookingId);

        // Security check
        if (!booking.getTutorProfile().getUser().getId().equals(tutorUser.getId())) {
            throw new SecurityException("You are not authorized to reject this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setStatusNote(reason);
        return bookingRepository.save(booking);
    }

    /**
     * Mark a booking as completed after the session is done.
     * Called by tutor or admin.
     * After this, the student can leave a review.
     */
    public Booking completeBooking(Long bookingId) {
        Booking booking = findById(bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can be marked as completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    /**
     * Cancel a booking.
     * Can be done by student (if PENDING), tutor, or admin (any status).
     *
     * @param bookingId the booking to cancel
     * @param reason    reason for cancellation
     */
    public Booking cancelBooking(Long bookingId, String reason) {
        Booking booking = findById(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStatusNote(reason);
        return bookingRepository.save(booking);
    }

    /**
     * Admin can set booking to any status directly.
     * Full override capability for the admin panel.
     */
    public Booking adminUpdateBookingStatus(Long bookingId, BookingStatus newStatus, String note) {
        Booking booking = findById(bookingId);
        booking.setStatus(newStatus);
        booking.setStatusNote(note);
        return bookingRepository.save(booking);
    }

    // =====================================================
    // QUERY METHODS
    // =====================================================

    @Transactional(readOnly = true)
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
    }

    /** Student: get all their bookings */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStudent(StudentProfile studentProfile) {
        return bookingRepository.findByStudentProfileOrderByCreatedAtDesc(studentProfile);
    }

    /** Tutor: get all bookings assigned to them */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByTutor(TutorProfile tutorProfile) {
        return bookingRepository.findByTutorProfileOrderByCreatedAtDesc(tutorProfile);
    }

    /** Tutor: get pending bookings that need a response */
    @Transactional(readOnly = true)
    public List<Booking> getPendingBookingsForTutor(TutorProfile tutorProfile) {
        return bookingRepository.findByTutorProfileAndStatus(tutorProfile, BookingStatus.PENDING);
    }

    /** Admin: get all bookings */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /** Admin: get bookings filtered by status */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /** Admin dashboard: count stats */
    @Transactional(readOnly = true)
    public long countByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countTodayBookings() {
        return bookingRepository.countByBookingDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long countAllBookings() {
        return bookingRepository.count();
    }
}
