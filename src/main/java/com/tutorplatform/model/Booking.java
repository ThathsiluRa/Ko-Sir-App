package com.tutorplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * BOOKING ENTITY
 * =====================================================
 * The central entity of the platform - represents a tutoring session
 * booked between a student and a tutor.
 *
 * BOOKING FLOW:
 * 1. Student searches for a tutor on /student/search
 * 2. Student clicks "Book" → fills out booking form
 * 3. Booking created with status=PENDING
 * 4. Tutor sees it on their dashboard and CONFIRMS or REJECTS
 * 5. After session, Admin or Tutor marks it COMPLETED
 * 6. Student can now leave a REVIEW
 *
 * DATABASE TABLE: "bookings"
 *
 * RELATIONSHIPS:
 * - Many Bookings → One StudentProfile (many bookings per student)
 * - Many Bookings → One TutorProfile (many bookings per tutor)
 * - One Booking → One Review (optional, only if completed)
 * =====================================================
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // WHO IS INVOLVED
    // =====================================================

    /**
     * The student who made this booking.
     * @ManyToOne: Many bookings can belong to one student.
     * @JoinColumn: The FK "student_profile_id" is in the bookings table.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    /**
     * The tutor being booked.
     * @ManyToOne: Many bookings can be assigned to one tutor.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    private TutorProfile tutorProfile;

    // =====================================================
    // WHAT SESSION DETAILS
    // =====================================================

    /**
     * Subject for this specific session.
     * Example: "Mathematics - Algebra"
     * A tutor might teach multiple subjects; student picks one per booking.
     */
    @NotBlank(message = "Please specify the subject for this session")
    @Column(nullable = false)
    private String subject;

    /**
     * The date the session is scheduled.
     * LocalDate = date without time (e.g., 2025-06-15)
     */
    @NotNull(message = "Please select a date for the session")
    @Column(nullable = false)
    private LocalDate bookingDate;

    /**
     * What time the session starts.
     * LocalTime = time without date (e.g., 14:30)
     */
    @NotNull(message = "Please select a start time")
    @Column(nullable = false)
    private LocalTime startTime;

    /**
     * How long the session lasts (in hours).
     * Example: 2 = 2-hour session
     */
    @NotNull(message = "Please select session duration")
    @Column(nullable = false)
    private Integer durationHours;

    /**
     * Total cost = tutorProfile.hourlyRate * durationHours
     * Calculated automatically when the booking is created.
     * Stored in DB so price doesn't change if tutor updates their rate.
     */
    @Column(nullable = false)
    private BigDecimal totalPrice;

    /**
     * Where the session takes place.
     *
     * For HOME_VISIT tutors: the student's home address (entered at booking time).
     * For FIXED_LOCATION tutors: the tutor's teaching place address (copied from TutorProfile).
     * For BOTH: whichever option the student chose.
     *
     * This is captured at booking time and stored here so it never changes
     * even if the tutor later updates their teachingAddress.
     */
    @Column(length = 500)
    private String sessionLocation;

    /**
     * Any special notes from the student.
     * Example: "Please focus on quadratic equations for the exam"
     */
    @Column(length = 500)
    private String notes;

    // =====================================================
    // STATUS TRACKING
    // =====================================================

    /**
     * Current status of this booking.
     * Stored as String in DB (e.g., "PENDING").
     * See BookingStatus enum for all possible values and their flow.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    /**
     * Reason for cancellation or rejection (optional).
     * Admin/Tutor can add a note when rejecting or cancelling.
     */
    @Column(length = 300)
    private String statusNote;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    /** When the booking was first created */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** When the booking was last updated (status change, etc.) */
    @Column
    private LocalDateTime updatedAt;

    // =====================================================
    // RELATIONSHIP TO REVIEW
    // A review is optional - only created after completion.
    // @OneToOne with mappedBy means the Review table holds the FK.
    // =====================================================
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Review review;

    // =====================================================
    // JPA LIFECYCLE HOOKS
    // =====================================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Default status is PENDING when first created
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Checks if a review can be submitted for this booking.
     * Review is only allowed after the session is COMPLETED
     * and only if no review has been submitted yet.
     */
    public boolean canLeaveReview() {
        return this.status == BookingStatus.COMPLETED && this.review == null;
    }

    /**
     * Calculates the total price based on tutor's hourly rate.
     * Call this when creating a new booking.
     */
    public void calculateTotalPrice() {
        if (this.tutorProfile != null && this.durationHours != null) {
            this.totalPrice = this.tutorProfile.getHourlyRate()
                    .multiply(BigDecimal.valueOf(this.durationHours));
        }
    }

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public Booking() {}

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StudentProfile getStudentProfile() { return studentProfile; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }

    public TutorProfile getTutorProfile() { return tutorProfile; }
    public void setTutorProfile(TutorProfile tutorProfile) { this.tutorProfile = tutorProfile; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getSessionLocation() { return sessionLocation; }
    public void setSessionLocation(String sessionLocation) { this.sessionLocation = sessionLocation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
}
