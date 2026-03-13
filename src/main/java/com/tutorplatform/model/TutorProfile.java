package com.tutorplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * TUTOR PROFILE ENTITY
 * =====================================================
 * Stores all tutor-specific information beyond basic user data.
 * Every User with role=TUTOR should have exactly one TutorProfile.
 *
 * This profile contains:
 * - Professional info (subjects, qualifications, experience)
 * - Business info (hourly rate, location, availability)
 * - Platform stats (average rating, total reviews)
 * - Admin approval status
 *
 * DATABASE TABLE: "tutor_profiles"
 *
 * RELATIONSHIPS:
 * - TutorProfile → User (one-to-one, tutor_profile owns the FK)
 * - TutorProfile → List<Booking> (one tutor can have many bookings)
 * - TutorProfile → List<Review> (one tutor can have many reviews)
 * =====================================================
 */
@Entity
@Table(name = "tutor_profiles")
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // RELATIONSHIP TO USER
    // @OneToOne: Each tutor profile belongs to exactly one user.
    // @JoinColumn: The FK "user_id" lives in the tutor_profiles table.
    // =====================================================
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =====================================================
    // PROFESSIONAL INFORMATION
    // =====================================================

    /**
     * Subjects the tutor teaches.
     * Example: "Mathematics, Physics, Chemistry"
     * We store as a simple String for simplicity.
     * In a production app you'd use a separate Subject table.
     */
    @NotBlank(message = "Please list the subjects you teach")
    @Column(nullable = false)
    private String subjects;

    /**
     * Short biography introducing the tutor.
     * Displayed on the tutor's public profile card.
     */
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    @Column(length = 1000)
    private String bio;

    /**
     * Educational qualification.
     * Example: "BSc Mathematics, University of Nairobi"
     */
    @Column
    private String qualification;

    /**
     * Years of teaching experience.
     * Example: 5 (means 5 years)
     */
    @Min(value = 0, message = "Experience cannot be negative")
    @Column
    private Integer experienceYears;

    // =====================================================
    // BUSINESS INFORMATION
    // =====================================================

    /**
     * Cost per hour for a tutoring session.
     * BigDecimal is used for money (never use float/double for currency!)
     */
    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.0", message = "Hourly rate cannot be negative")
    @Column(nullable = false)
    private BigDecimal hourlyRate;

    /**
     * General area the tutor operates in.
     * Example: "Colombo — Nugegoda, Maharagama"
     * Used for search display and filtering.
     */
    @Column
    private String location;

    /**
     * Tutor's available times.
     * Example: "Weekdays 4pm-8pm, Weekends 9am-5pm"
     */
    @Column
    private String availability;

    /**
     * How the tutor delivers sessions.
     *
     * "HOME_VISIT"      → Tutor travels to the student's home.
     *                     The student must provide their home address when booking.
     *
     * "FIXED_LOCATION"  → Student comes to the tutor's dedicated teaching place.
     *                     The tutor provides teachingAddress; it is shown to the student.
     *
     * "BOTH"            → Tutor offers both options; student chooses at booking.
     *
     * Default is HOME_VISIT (most common for home tutoring platforms).
     */
    @Column(nullable = false)
    private String teachingMode = "HOME_VISIT";

    /**
     * The tutor's dedicated teaching place address.
     * Only relevant when teachingMode is FIXED_LOCATION or BOTH.
     * Example: "123 Galle Road, Colombo 03 (near Unity Plaza)"
     * Shown to the student when they book so they know where to go.
     */
    @Column(length = 500)
    private String teachingAddress;

    // =====================================================
    // PLATFORM STATISTICS (auto-calculated)
    // =====================================================

    /**
     * Average rating from all reviews (1.0 to 5.0).
     * Updated automatically whenever a new review is submitted.
     */
    @Column
    private Double averageRating = 0.0;

    /**
     * Total number of reviews received.
     * Used to display "based on N reviews" text.
     */
    @Column
    private Integer totalReviews = 0;

    // =====================================================
    // ADMIN CONTROL FLAGS
    // =====================================================

    /**
     * Whether the admin has approved this tutor.
     * New tutors default to NOT approved (false).
     * Admin must approve them before they appear in student searches.
     * This prevents fake/unqualified tutors from joining.
     */
    @Column(nullable = false)
    private boolean approved = false;

    /**
     * Whether this tutor is currently accepting new bookings.
     * Tutor can toggle this on/off from their dashboard.
     */
    @Column(nullable = false)
    private boolean acceptingBookings = true;

    // =====================================================
    // RELATIONSHIPS TO OTHER ENTITIES
    // mappedBy = the field in Booking/Review that owns the FK
    // CascadeType.ALL = when tutor is deleted, delete their bookings too
    // =====================================================

    @OneToMany(mappedBy = "tutorProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "tutorProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public TutorProfile() {}

    public TutorProfile(User user, String subjects, BigDecimal hourlyRate, String location) {
        this.user = user;
        this.subjects = subjects;
        this.hourlyRate = hourlyRate;
        this.location = location;
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Recalculates the average rating after a new review is added.
     * Called by ReviewService whenever a review is saved.
     * @param newRating the rating from the new review (1-5)
     */
    public void addReviewRating(int newRating) {
        // Calculate new average: (old total + new rating) / (old count + 1)
        double currentTotal = this.averageRating * this.totalReviews;
        this.totalReviews++;
        this.averageRating = (currentTotal + newRating) / this.totalReviews;
        // Round to 1 decimal place
        this.averageRating = Math.round(this.averageRating * 10.0) / 10.0;
    }

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSubjects() { return subjects; }
    public void setSubjects(String subjects) { this.subjects = subjects; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getTeachingMode() { return teachingMode; }
    public void setTeachingMode(String teachingMode) { this.teachingMode = teachingMode; }

    public String getTeachingAddress() { return teachingAddress; }
    public void setTeachingAddress(String teachingAddress) { this.teachingAddress = teachingAddress; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean isAcceptingBookings() { return acceptingBookings; }
    public void setAcceptingBookings(boolean acceptingBookings) { this.acceptingBookings = acceptingBookings; }

    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}
