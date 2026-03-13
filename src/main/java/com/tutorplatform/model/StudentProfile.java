package com.tutorplatform.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * STUDENT PROFILE ENTITY
 * =====================================================
 * Stores student-specific details beyond the basic User account.
 * Every User with role=STUDENT has one StudentProfile.
 *
 * This captures:
 * - Academic level (grade/year)
 * - School name
 * - Parent/guardian contact info (important for minors!)
 *
 * DATABASE TABLE: "student_profiles"
 *
 * RELATIONSHIPS:
 * - StudentProfile → User (one-to-one)
 * - StudentProfile → List<Booking> (one student can make many bookings)
 * =====================================================
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // RELATIONSHIP TO USER ACCOUNT
    // =====================================================
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =====================================================
    // ACADEMIC INFORMATION
    // =====================================================

    /**
     * Student's current grade or education level.
     * Examples: "Grade 7", "Form 3", "Year 10", "A-Level"
     */
    @Column
    private String grade;

    /**
     * Name of the student's school.
     * Example: "Nairobi Academy"
     */
    @Column
    private String school;

    /**
     * Subjects the student needs help with.
     * Example: "Mathematics, English, Science"
     */
    @Column
    private String subjectsNeeded;

    // =====================================================
    // HOME ADDRESS
    // Required when a tutor offers home visits (teachingMode = HOME_VISIT or BOTH).
    // The tutor needs this to know where to go for the session.
    // =====================================================

    /**
     * Student's full home address (street, house number, area).
     * Example: "45/B Baseline Road, Colombo 08"
     * Shared with the tutor once a booking is confirmed.
     */
    @Column(length = 500)
    private String address;

    /**
     * Student's city.
     * Example: "Colombo", "Kandy", "Galle"
     */
    @Column
    private String city;

    // =====================================================
    // PARENT / GUARDIAN INFORMATION
    // This is especially important for young students.
    // The platform can send notifications to the parent.
    // =====================================================

    /**
     * Parent or guardian's full name.
     */
    @Column
    private String parentName;

    /**
     * Parent or guardian's phone number.
     * Used for booking confirmations and emergencies.
     */
    @Column
    private String parentPhone;

    // =====================================================
    // BOOKINGS - all sessions this student has booked
    // =====================================================
    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public StudentProfile() {}

    public StudentProfile(User user, String grade, String school) {
        this.user = user;
        this.grade = grade;
        this.school = school;
    }

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getSubjectsNeeded() { return subjectsNeeded; }
    public void setSubjectsNeeded(String subjectsNeeded) { this.subjectsNeeded = subjectsNeeded; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }
}
