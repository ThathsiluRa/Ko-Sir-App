package com.tutorplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * REVIEW ENTITY
 * =====================================================
 * A review is written by a student after a COMPLETED booking.
 * Reviews help other students choose the right tutor.
 *
 * RULES:
 * - Only one review per booking (enforced by @OneToOne)
 * - Only COMPLETED bookings can have reviews
 * - Only the student who made the booking can review it
 * - Admin can delete any review from the admin panel
 *
 * DATABASE TABLE: "reviews"
 *
 * RELATIONSHIPS:
 * - Review → Booking (one review per booking)
 * - Review → User (the student who wrote it)
 * - Review → TutorProfile (the tutor being reviewed)
 * =====================================================
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // WHAT IS BEING REVIEWED
    // =====================================================

    /**
     * The booking this review is for.
     * @OneToOne enforces: one booking can only have ONE review.
     * @JoinColumn: FK "booking_id" is in the reviews table.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /**
     * The tutor profile being reviewed.
     * We store this directly for easy querying of "all reviews for tutor X".
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    private TutorProfile tutorProfile;

    /**
     * The student user who wrote this review.
     * We reference User directly (not StudentProfile) for simplicity.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    // =====================================================
    // REVIEW CONTENT
    // =====================================================

    /**
     * Star rating from 1 to 5.
     * @Min and @Max enforce the valid range.
     * 1 = Very poor, 5 = Excellent
     */
    @NotNull(message = "Please select a rating")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(nullable = false)
    private Integer rating;

    /**
     * Written feedback about the tutor.
     * Optional but encouraged.
     * Example: "Mr. John is an excellent teacher who explains concepts clearly."
     */
    @Column(length = 1000)
    private String comment;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    /** When the review was submitted */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public Review() {}

    public Review(Booking booking, TutorProfile tutorProfile, User student, int rating, String comment) {
        this.booking = booking;
        this.tutorProfile = tutorProfile;
        this.student = student;
        this.rating = rating;
        this.comment = comment;
    }

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public TutorProfile getTutorProfile() { return tutorProfile; }
    public void setTutorProfile(TutorProfile tutorProfile) { this.tutorProfile = tutorProfile; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
